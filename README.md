# PodAutoScaler
This application is used to adjust the scaling of kubernetes deployments
depending on the message-count of given RabbitMQ queues.

It uses a YAML configuration where you can set all queue--deployment relations
with the scaling-rules.

### Deployment with Helm

To deploy this application using helm, you simply write a ``values.yaml`` and use the command described below.

````yaml
image:
  repository: mfhelm/podautoscaler
  pullPolicy: Always
  version: 1.0.1

rabbitmqAccessSecret: <name of the secret for RabbitMQ (*1)>
rabbitmqUser: <username for RabbitMQ-Server>
rabbitmqHost: <host of RabbitMQ-Server>
rabbitmqPort: <port of RabbitMQ-Server>

logTrace: false

scalerConfig: |
  <scalerConfig>
````

*1: The secret must set the environment-variable ``rabbitmq-password`` to the password for the RabbitMQ-User.

The value ``scalerConfig`` must contain the string of the config-yaml.
It can be written directly in the values.yaml or read from a file by appending
``--set-file scalerConfig=<path to config.yaml>`` to the helm-upgrade-command
(in this case you must remove the last two lines).

Optionally tracing of rule-execution can be enabled by setting ``logTrace: true``.

---
To deploy the application, first the mayope helm-repository mus be added.
Then you can install it with the second command.
````shell
helm repo add mayope https://charts.mayope.net

helm upgrade --install <name of deployment> mayope/podautoscaler -f values.yaml -n <namespace to deploy to>
````

### Configuration

The configuration has the following format:
````yaml
-
  label: "<name for this conf entry (e.g. for logs)>; optional (default is '_unnamed_')"
  deploymentNamespace: "<kubernetes namespace>; optional (*2)"
  deployment: "<name of the deployment to scale>"
  interval: "<interval in which the queue should be checked>; number | string>"
  queues:
    -
      virtualHost: "<path to the virtual host for the queue (including leading slash)>; optional (default is '/')"
      name: "<name of the RabbitMQ queue to observe>"
      ruleset:
          type: "<limit | linearScale | logScale; sets the type of the rules>"
          rules:
            - # type = limit
              minMessageCount: "<minimum count of messages to trigger this limit>; number | string"
              podCount: "<number of pods to run when this limit is triggered>; number"
            - # type = linearScale
              factor: "<podCount = factor * messageCount>; number"
              stepThreshold: "<min diff of podCount to trigger update>; number; optional (default is 1)"
              minPodCount: "<lower limit for podCount>; number; optional (default: 1)"
              maxPodCount: "<upper limit for podCount>; number; optional (default: 10)"
            - # type = logScale
              base: "<podCount = log(messageCount, base) + offset>; number"
              offset: "<number; optional (default is 0)>"
              stepThreshold: "<min diff of podCount to trigger update>; number; optional (default is 1)"
              minPodCount: "<lower limit for podCount>; number; optional (default: 1)"
              maxPodCount: "<upper limit for podCount>; number; optional (default: 10)"
````

- *2: default is the value of the java-property 'namespace' or (if not set) the value of the environment-var 'NAMESPACE'
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

| Message-Count    | new Pod-Count |
|------------------|---------------|
| 0 <= mc < 500    | 1             |
| 500 <= mc < 2000 | 2             |
| mc >= 2000       | 5             |

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

This example is similar to the last. The difference is the interval (which is 5 minutes) and the rules.
For the rules here are some example-calculations:

| Message-Count | current Pod-Count | new Pod-Count | Notes                                                                                        |
|---------------|-------------------|---------------|----------------------------------------------------------------------------------------------|
| mc = 490      | pc = 1            | 5             | pod-count will be rounded                                                                    |
| mc = 700      | pc = 1            | 5             | because ``maxPodCount`` is 5                                                                 |
| mc = 100      | pc = 2            | 2             | because ``stepThreshold`` is 2 and the new pod-count would be 1 (pc - 1 < ``stepThreshold``) |

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

| Message-Count | current Pod-Count | new Pod-Count | Notes                                                                                        |
|---------------|-------------------|---------------|----------------------------------------------------------------------------------------------|
| mc = 2        | pc = 5            | 1             | new Pod-Count will be rounded and ``minPodCount`` is 1                                       |
| mc = 1000     | pc = 1            | 3             |                                                                                              |
| mc = 30000    | pc = 5            | 5             | because ``stepThreshold`` is 2 and the new Pod-Count would be 4 (pc - 4 < ``stepThreshold``) |

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

| Queue-A Message-Count | Queue-B Message-Count | current Pod-Count | new Pod-Count |
|-----------------------|-----------------------|-------------------|---------------|
| 100                   | 100                   | cp = 1            | 2             |
| 100                   | 1000                  | cp = 1            | 5             |
