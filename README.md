# PodAutoScaler
This application is used to adjust the scaling of kubernetes deployments
depending on the message-count of given RabbitMQ queues.

It uses a YAML configuration where you can set all queue--deployment relations
with the scaling-rules.

### Configuration

````yaml
-
  label: "<name for this conf entry (e.g. for logs)>; optional (default is '_unnamed_')"
  deploymentNamespace: "<kubernetes namespace>; optional (default is value of environment-var 'NAMESPACE')"
  deployment: "<name of the deployment to scale>"
  interval: "<interval in which the queue should be checked>; number | string>"
  queues:
    -
      virtualHost: "<path to the virtual host for the queue (including leading slash)>; optional (default is '/')"
      name: "<name of the RabbitMQ queue to observe>"
      ruleset:
          type: "<limit | linearScaling | logScaling; sets the type of the rules>"
          rules:
            - # type = limit
              minMessageCount: "<minimum count of messages to trigger this limit>; number | string"
              podCount: "<number of pods to run when this limit is triggered>; number"
            - # type = linearScaling
              factor: "<podCount = factor * messageCount>; number"
              stepThreshold: "<min diff of podCount to trigger update>; number; optional (default is 1)"
              minPodCount: "<lower limit for podCount>; number; optional (default: 1)"
              maxPodCount: "<upper limit for podCount>; number; optional (default: 10)"
            - # type = logScaling
              base: "<podCount = log(messageCount, base)>; number"
              stepThreshold: "<min diff of podCount to trigger update>; number; optional (default is 1)"
              minPodCount: "<lower limit for podCount>; number; optional (default: 1)"
              maxPodCount: "<upper limit for podCount>; number; optional (default: 10)"
````

- value of `interval`: can be a number (in seconds) or a string of the following format &lt;value&gt;s|m|h|d 
    where s means seconds, m minutes, h hours and d days
- limit ruleset:
  - must have at least one rule
  - if there is nor rule with `minMessageCount` = 0 then one will be created with `podCount` of the rule with the smallest `minMessageCount`
  - value of `minMessageCount`: can be a number or a string of the following format &lt;value&gt;k|m where k means thousand an m million
- linearScale ruleset:
  - must have exactly one rule
- logScale ruleset:
  - must have exactly one rule
- types of rules can not be mixed

The configuration is passed to the application via the environment-variable `config` (must be a string with the YAML content).

#### Example with limit

````yaml
-
  label: "example_with_limit"
  deploymentNamespace: ns-1
  deployment: example
  interval: 60
  queues:
    -
      virtualHost: /hostA
      name: queue_a
      ruleset:
          type: limit
          rules:
            -
              minMessageCount: 0
              podCount: 1
            -
              minMessageCount: 500
              podCount: 2
            -
              minMessageCount: 2k
              podCount: 5
````

This example will observe the queue 'queue_a' on virtual-host '/hostA'
and scale the deployment 'example' in namespace 'ns-1' according to the following rules:
- 0 <= message-count < 500 => 1 pod
- 500 <= message-count < 2000 => 2 pods
- message-count >= 2000 => 5 pods

#### Example with linearScale

````yaml
-
  label: "example_with_linearScale"
  deploymentNamespace: ns-1
  deployment: example
  interval: 5m
  queues:
    -
      virtualHost: /hostA
      name: queue_a
      ruleset:
          type: linearScale
          rules:
            -
              factor: 0.01
              stepThreshold: 2
              minPodCount: 1
              maxPodCount: 5
````

This example is similar to the last. The difference is the interval, which is 5 minutes and the rules.
For the rules here are some example-calculations:
- message-count = 490, current-pod-count = 1 => 5 pods (pod-count will be rounded)
- message-count = 700, current-pod-count = 1 => 5 pods (because ``maxPodCount`` is 5)
- message-count = 100, current-pod-count = 2 => 2 pods (because ``stepThreshold`` is 2 and the new pod-count would be 1)

#### Example with logScale

````yaml
-
  label: "example_with_logScale"
  deploymentNamespace: ns-1
  deployment: example
  interval: 5m
  queues:
    -
      virtualHost: /hostA
      name: queue_a
      ruleset:
          type: logScale
          rules:
            -
              base: 10
              stepThreshold: 2
              minPodCount: 1
              maxPodCount: 5
````

- message-count = 2, current-pod-count = 5 => 1 pods (pod-count will be rounded and ``minPodCount`` is 1)
- message-count = 1000, current-pod-count = 1 => 3 pods
- message-count = 30000, current-pod-count = 5 => 5 pods (because ``stepThreshold`` is 2 and the new pod-count would be 4)

#### Example with multiple queues

````yaml
-
  label: "example_with_multi_queue"
  deploymentNamespace: ns-1
  deployment: example
  interval: 5m
  queues:
    -
      virtualHost: /hostA
      name: queue_a
      ruleset:
        type: limit
        rules:
          -
            minMessageCount: 0
            podCount: 1
          -
            minMessageCount: 100
            podCount: 2
          -
            minMessageCount: 200
            podCount: 3
    -
      virtualHost: /hostB
      name: queue_b
      ruleset:
        type: linearScale
        rules:
          - factor: 0.005
````

It is also possible to observe more than one queue per deployment. Each queue has its own ruleset
which calculates the new pod-count. The final pod-count is the maximum of all the calculated count of the queue-rulesets.

An example:
- queue_a-count = 100, queue_b-count = 100, current-pod-count = 1 => 2 pods
- queue_a-count = 100, queue_b-count = 1000, current-pod-count = 1 => 5 pods
