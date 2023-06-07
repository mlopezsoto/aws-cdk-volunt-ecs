# Java AWS CDK Stack for a Spring Boot App that uses a Database
This AWS CDK Stack deploys a Spring Boot based app to ECS. It assumes the image is a Spring boot that connects to a database.

## Requirements
You would need a AWS Java CDK project.

## Stack Steps
* VPN creation with two subnets, one public one isolated.
* Postgres Database creation. 
* ECS Fargate Cluster creation.


## Usage example

```java
App app = new App();

AwsCdkSpringPostgresEcsFargateStack.Builder.create(app, "butterfly", "butterflydb", "butter", "flyingbutter", "docker.io/mlopezsoto/volunt:latest")
.ecsTaskCpu(512)
.ecsTaskMemoryLimitMiB(512)
.ecsTaskDesiredCount(2)
.springContainerPort(8080)
.targetGroupHealthCheckPath("/actuator/health")
.vpcMaxAzs(3)
.build();

app.synth();
```

## Sample App
The image 'docker.io/mlopezsoto/volunt:latest' contains a basic Spring Boot app that creates a user table
and exposes a couple of endpoints:

- /user/listAll
- /user/validateCredentials?username=<username>&password=<password>

You can test this using the CDK output 'butterfly.ALBDNSName' and then appending the above paths.
Stack Output example:

```
Outputs:
butterfly.ALBDNSName = butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
butterfly.DatabaseAddress = butterfly-database-postgresql.cmwkrmicwxfc.ap-southeast-2.rds.amazonaws.com
butterfly.DatabasePort = 5432
butterfly.butterflyecsserviceLoadBalancerDNS7FF68FBE = butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
butterfly.butterflyecsserviceServiceURL1D691D62 = http://butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
butterfly.datasourceUrl = jdbc:postgresql://butterfly-database-postgresql.cmwkrmicwxfc.ap-southeast-2.rds.amazonaws.com:5432/butterflydb
```

butterfly.ALBDNSName = butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com
then: http://butte-butte-DZD07FFTQKI5-224749273.ap-southeast-2.elb.amazonaws.com/user/listAll

App code: https://github.com/mlopezsoto/volunt-be

## Basic CDK commands
### List all Stacks
> cdk ls
### Deploy a stack, for instance a stack named 'butterfly' as in the sample code above
> cdk deploy butterfly
### Destroy (undeploy) a stack, for instance a stack named 'butterfly' as in the sample code above
> cdk destroy butterfly