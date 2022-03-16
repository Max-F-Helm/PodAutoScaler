# PodAutoScaler
This small application is used to adjust the scaling of a kubernetes deployment
depending on the message-count of a given RabbitMQ queue.

It uses a YAML configuration where you can set all queue--deployment relations
with the scaling-rules.

### Configuration

````yaml
-
  label: "<name for this conf entry (e.g. for logs)>; optional (default is '_unnamed_')"
  queueVirtualHost: "<path to the virtual host for the queue (including leading slash)>; optional (default is '/')"
  queueName: "<name of the RabbitMQ queue to observe>"
  deploymentNamespace: "<kubernetes namespace>; optional (default is value of environment-var 'NAMESPACE')"
  deployment: "<name of the deployment to scale>"
  interval: "<interval in which the queue should be checked>; number | string>"
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
