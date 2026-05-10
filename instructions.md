# Android apk for km registration

I have my own business and want to have my car km registrated. I want to build my own app for that. 

# Android auto
When i get in my car, i connect my phone to android auto. I want the app to notice this and create a pop-up on the android auto (aa) screen where i can select if the drive is business or private. It has to store my location startpoint and endpoint. I have an volvo application (or perhaps an api from volvo) in which my (total)km is registred. 

In case of disconnection (connection is with usb and not always stable) and reconnection within 1 minute, the pop-up should offer a continuing of the drive, or start a new drive.

A: connects phone to car, android auto is connected
B: pop-up on aa screen with the selection of private or business
C: only in case of a connection with a privious connection within 1 minute, i should also have the option to continue the same drive 


In all cases, startpoint (location and time) and endpoint should be registred.
This should be easy to export to csv or xsl format for the books. 
I want an apk file i can install on my phone. 
I consider deploying a server to store and sync the data


# Data collection
1. Get location from phone
2. Get date and time from phone
3. select private/business
4. Get current km value from volvo api (i got a paid subscription for the volvo app) 
5. Store data
6. Sync data (optional)
7. when disconnect > store current position
8. if reconnect within 60 sec, offer to continue the current drive 
9. when starting a new drive, save current km value from volvo (this should be the same value as the end point of the previous drive and also same location
    - if this is not the case, make sure to highlight an error and mark the row red in the csv or xsl
 
