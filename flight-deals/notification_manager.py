from twilio.rest import Client
import os


class NotificationManager:
    def __init__(self, body):
        self.client = Client(os.environ['account_sid'], os.environ['auth_token'])
        message = self.client.messages.create(
            from_=os.environ['from'],
            to=os.environ['to'],
            body=body
        )
        print(message.sid)

