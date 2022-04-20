# Overview
## Beckn Open Network

Beckn Open network is a playground for developers building applications that
use the beckn protocol. It is a test network that consists of a hosted
{{ '[registry]({})'.format(rg) }} 
and  
{{ '[gateway]({})'.format(gw) }}
.Since, It is not a production network , there is no commercial or financial liability imposed on
any platform to fulfill any contract established on this network.


## Registering on the Network
1. Register yourself at the 
{{ '[registry]({})'.format(rg) }} 
by clicking ![Google](https://upload.wikimedia.org/wikipedia/commons/5/53/Google_%22G%22_Logo.svg "Google") icon and using your gmail id. 
1. Now, 
    * Register  Your company's fully qualified domain name (fqdn) as a Network Participant by clicking the <a class="fa fa-plus"></a> button
        {{ '[here]({}/network_participants)'.format(rg) }} 
    * Generate for your application platform (outside beckn-one), a Ed25518  key pair for signing and X25518 key pair for encryption purposes.  
	    * Keep the private keys with you safely and register the public keys on **Participant Key** tab against your company's entry  as a **Network Participant**. 
	    * If you want to simply play around, you can generate the key pairs on beckn one by clicking on the         <i class="fa fa-key"></i> icon  on the **Network Participant Information** tab. 
	    * To know  you private keys you can see {{ '[here]({}/crypto_keys)'.format(rg) }}
    * Register the domain and roles you wish to subscribe on the network by clicking the <i class="fa fa-plus"></i> button on the **Network Role Tab**. 
        * Specify end points for your subscription and mark yourself in "SUBSCRIBED" status.      
*Note: If you wish to test your registry integration with the /subscribe api , keep the status as "INITIATED". It would get marked as "SUBSCRIBED" after your endpoint resolves the registry's challenge successfully.*

## The Registy Apis
There Registry primarily has 2 apis lookup and subscribe. 
To know more about how to use them read [about registry apis](reg_apis_rehash.md)

