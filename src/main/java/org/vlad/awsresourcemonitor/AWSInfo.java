package org.vlad.awsresourcemonitor;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityRequest;
import com.amazonaws.services.securitytoken.model.GetCallerIdentityResult;

/**
 * Singleton with AWS Session info
 */
public enum  AWSInfo {
  INSTANCE;

  private final DefaultAWSCredentialsProviderChain credProvider;
  private final AmazonEC2Client ec2;
  private final AmazonRDSClient rds;
  private final AWSSecurityTokenService sts;
  private final String acc;


  AWSInfo() {
    credProvider = new DefaultAWSCredentialsProviderChain();
    sts = new AWSSecurityTokenServiceClient(credProvider);
    ec2 = new AmazonEC2Client(credProvider);
    rds = new AmazonRDSClient(credProvider);

    GetCallerIdentityRequest getCallIdRq = new GetCallerIdentityRequest();
    GetCallerIdentityResult callIdRes = sts.getCallerIdentity(getCallIdRq);

    acc = callIdRes.getAccount();


  }

  public static AmazonRDSClient getRds() {
    return INSTANCE.rds;
  }

  public static AWSSecurityTokenService getSts() {
    return INSTANCE.sts;
  }

  public DefaultAWSCredentialsProviderChain getCredProvider() {
    return credProvider;
  }

  public static AmazonEC2Client getEc2() {
    return INSTANCE.ec2;
  }

  public static String getAcc() {
    return INSTANCE.acc;
  }
}
