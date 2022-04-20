# Beckn Registry Apis

## /subscribe 

Complete Payload
-
While complete payload is elaborate, based on usecase, only some of them may be needed in any call. 
 
	{ 
 		subscriber_id:"" ,"subscriber_url": ""	, domain : "" ,  type :"" , status :"" ,  
 		unique_key_id: "" , signing_public_key:"" , encr_public_key :"" , valid_from:"" , 
 		valid_until:"" ,   
 		country:"" , city: "" , "lat" :"" , "lng" :"" , "radius" :"" , 		nonce: "" 
	}

### Flows: 
#### Registration 
Often registration is an offline process. An entity intending to participate in a beckn network, approaches the registrar of the network. 

The registrar: 

##### Takes the following details from the entity:
	
1. participant_id to identify the entity (it could be an fqdn/pan/gstin or any other possible unique id)
1. A Ed25519 public key for signing and  X25519 public key to be used for derieved an aes key (using the  Registry's private key and the entity's public key) for the purpose of encryption.
1. domains intending to subscribe to (nic codes), the roles (type) the participant wishes to participate and their corresponding urls which would be used for invoking beckn transactions and registry verification hooks (/on_subscribe) on the subscriber's application. 
1. Areas of service (city,region,...) by each domain the entity wishes to operate in.
 

##### Provides the following to the entity
1. A key Id is generated to identify "the initial keyset (signing and encryption keys)" and is handed over to the subscriber. This key Id is marked as verified. 

#### Getting the participant "subscribed".
Registrar asks the subscriber to keep their sites (all registered urls for various domains) up for verification purposes. 
1. Registry would issue a signed /on_subscribe call on each of the ${subscriber_url}/on_subscribe endpoint with the following payload.
 
		{ "subscriber_id" :"" , unique_key_id" : "" , "challenge": "" }
		
1. Participant can verify the  registry signature (using registry's signing public key) and then resolve the decryption challenge given using the registry's encr public key and participant's encr private key (corresponding to the key id passed in the payload)
2. challenge is resolved and sent back
2. Registry checks if the challenge was correctly decrypted and makes the subscriber's subscription for the domain/type as "SUBSCRIBED"
1. If for any reason, this process didnot complete the participant can request a /on_subscribe by issuing an authorized /subscribe call on the registry with an empty payload.
		{}
1. The registry would call the subscription's ${url}/on_subscribe again based on this request if not in "SUBSCRIBED" status . If the  subscriber status is already "SUBSCRIBED", no further action will be taken other than passing the status as "SUBSCRIBED" to the subscriber in the response.  



#### Adding new keys.
1. subscriber would make an authorized call to "/subscribe or _/addkey ?_" on the registry url _The request must contain standard Authorization headers needed by the beckn protocol using a valid signature key_

		Payload being 
		{"unique_key_id" :"" , signing_public_key:"" , encr_public_key :"" , valid_from:"" , valid_until:"" , nonce: ""}

		unique_key_id needs to unique within the registry, the subscriber could use a UUID based scheme to generate this id before passing to the registry.


1. The registry would validate the Authorization header
2. 
		if the  unique_key_id is not existing
			create the entry for the key  and mark it invalid.
		else if the keyid is existing and invalid 
			update the details 
		else if unique_key_id  is valid
			Throw exception , already exists. .
		end
		
		Make a signed call to each of the ${registered_url}/on_subscribe registered against every domain/type registered passing this unique_key_id and a challenge to solve using this key
		{ "subscriber_id" :"" , unique_key_id" : "" , "challenge": "" }
		
		if challenge is resolved by every subscription url in "SUBSCRIBED" state {
			mark this key as "verified" This unique_key_id can now be used for 
			signing and encryption going forward 
		}

		

#### Adding new service regions for a domain 
Subscriber makes an authorized /subscribe request to the registry url with payload 

{ "_city_" :"" , "_country_" :"" }

Registry makes the changes requested for the signing subscriber.



#### Updating subscription url
Subscriber makes an authorized /subscribe request to the registry url with payload 

{  _url_ : "" } 
 
Registry makes the changes requested for the signed subscriber.

		update the subscriber's url

#### Invalidating a key 
Make an authorized call to ${registry_url}/subscribe

		{ "unique_key_id" : "" , "valid_until" : "" } 
		Pass valid_until to a current  time to expire the key. 
		
		Restrictions:  The unique_key_id being modified cannot be expired by the same unique_key_id used to authorize the call.  

The Key would be marked as invalidated. 

## /lookup
Api is an open api not requiring any authorizaion. 

### Complete payload:
		{ subscriber_id:"" , type:"" , domain: "" , city:"" , country: "" , unique_key_id:"" }

#### Lookup to get only keys; (_/lookup or /keys_)
Request: { unique_key_id: "" }

Response: [{ subscriber_id :"" , signing_public_key:"" , encr_public_key :"" , valid_from:"" , valid_until:"" , verified : "" }]

_0 or exactly one record returned._
 		
#### Lookup to get all information  (/lookup)
Request : { subscriber_id:"" , type:"" , domain: "" , city:"" , country: "" , "unique_key_id" : ""  }
_**None of the attributes are mandatory._

* get all matching subscriptions. 
* get all subscriptions for the ( (type = passed type or type = ' ')  and ( domain = passed domain or domain =  ' ') and ( subscriber_id = "passed subscriber id" ) and  ( ( no operating regions registered ) or (with regions where ( city =  passed city or ' ') and ( country = passed country or  ' ' )))
* make a list of valid keys. 
* if (unique_key_id is passed) {
		trim the list to contain only the passed key_id.
	}else {
		pick up the latest unique_key
	}

* Return the role,unique_key information in response	
Response: [{ subscriber_url : "" ,subscriber_id:"" type: "" , domain :"" ,city:"" , country: "" , unique_key_id:"", signing_public_key:"" , encr_public_key :"" , valid_from:"" , valid_until:"" , status: "" , created: "" , updated:"" }]

