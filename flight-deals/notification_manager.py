from twilio.rest import Client
import os


class NotificationManager:
    def __init__(self, body):
        self.client = Client(os.environ['account_sid'], os.environ['auth_token'])
        self.client.messages.create(
            from_='+18329240661',
            to='+8613128840500',
            body=body
        )