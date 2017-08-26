# MQTT Topics #
## Identify Topic ##
Main topic for any connected client is /chat/{customer_id}.  This allows each connected client to subscribe its own messages as well as publisher to send messages to the other client.

## Album Topic ##
"Album/{album_id}" is the dynamic topic created when a a user create an album while connected to the MQTT broker.   Owner also subscribes to the topic "Album/{album_id}" as well as other invited clients when they accept the invitation. 

## Topics table ##
Topics table will have 4 columns
* ID
* Customer ID
* Topic
* Confirmed
* Level of privilage 

Payloads 
Payload can be of 256 MB , however we should not reach that high loads. Payload will be encoded in AVRO binary son format. 

### Album creation ###
* Any one can create an album
* An entry is added for the Customer in topics table with its customer ID, Album/{album_id} and confirmed as true, privlages as admin.

```
{
	"event_type" : "create_album",
	"album_id":"customer_id_timestamp",
	"album_label": "My Holi 2017",
	"album_created_on" : "Date",
	"album_location": "GEO Cords",
	"album_owner" : "customer_id"

}
```


### Album deletion ###
* Only album owner can delete the album
* Delete all the rows which has topic as Album/{album_id} if customer has admin(need to discuss) .

```
{
	"event_type" : "delete_album",
	"album_id":"customer_id_timestamp",
	"album_delete_on" : "Date",
	"album,deleted_by" : "customer_id"
}
```


### Album update ###
* Only album owner can update the album
* Check if Customer has admin access to that Album/{album_id}.

```
{
	"event_type" : "update_album",
	"album_id":"customer_id_timestamp",
	"album_upated_on" : "Date",
	"album_updated_by" : "customer_id"

}
```

### Share album with a user ###
* Only album owner can add user to the album
* Check if Customer has share access to that Album/{album_id}.
```
{
    "event_type" : "add_user_to_album",
    "album_id":"customer_id_timestamp",
    "album_label": "My Holi 2017",
    "album_created_on" : "Date",
    "album_location": "GEO Cords",
    "album_owner" : "customer_id",
    "invitation_sender" : "customer_id",
    "invitation_receiver" : "customer_id",
    "invitee_privileges" : "Owner/Read/",
    "invitation_Message" : "Hi Tosheer, join this great album"
}
```

### Remove User from Album ###
* Only album owner can remove user from the album
* Check if Customer has share access to that topic Album/{album_id}. Remove from the table where removal_receiver and topic is Album/{album_id}.
```
{
    "event_type" : "remove_user_from_album",
    "album_id":"customer_id_timestamp",
    "removal_sender" : "customer_id",
    "removal_receiver" : "customer_id",
}
```

### Add a photo to an album ###
* Each participants can upload the photo
* Check if Customer has write access to that topic Album/{album_id}.
```
{
	"event_type" : "upload_photo",
    "album_id":"phonumber_timestamp",
    "photo_id" : "s3_id",
	"photo_caption": "great selfie",
	"photo_uploaded_on" : "Date",
	"photo_location": "GEO Cords",
	"photo_owner" : "phonenumber",
    "photo_thumb" : "base64 thumb"
}
```

### Remove a photo from an album ###
* A photo owner can delete the photo
* An album owner can delete the photo
* Can't check a photo level (need to discuss)
```
{
	"event_type" : "upload_photo",
    "album_id":"phonumber_timestamp",
    "photo_id" : "s3_id",
	"photo_caption": "great selfie",
	"photo_uploaded_on" : "Date",
	"photo_location": "GEO Cords",
	"photo_owner" : "phonenumber",
    "photo_thumb" : "base64 thumb"
}
```

### Comment on a photo ###
* Each participants can upload the photo
* Can't check a photo level (need to discuss)
```
{
	"event_type" : "comment_photo",
	"album_id":"phonumber_timestamp",
	"photo_id" : "s3_id",
	"comment": "great selfie",
	"commented_on" : "Date",
	"comment_by" : "phonenumber"
}
```

### Like on a photo ###
* Each participants can upload the photo
* Can't check a photo level (need to discuss)
```
{
	"event_type" : "like_photo",
	"album_id":"phonumber_timestamp",
	"photo_id" : "s3_id",
	"liked_on" : "Date",
	"liked_by" : "phonenumber"
}

```