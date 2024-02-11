import requests
import os
from twilio.rest import Client

api_key = os.environ.get("api_key")
my_longitude = 113.570438
my_latitude = 30.968713
account_sid = os.environ.get("account_sid")
auth_token = os.environ.get("auth_token")
send_to = os.environ.get("phone_number")

parameters= {
    "lat": my_latitude,
    "lon": my_longitude,
    "appid": api_key,
    "cnt": 4
}
response = requests.get(url="https://api.openweathermap.org/data/2.5/forecast", params=parameters)
response.raise_for_status()
weather_data = response.json()["list"]
will_rain = False
for weather in weather_data:
    condition_code = int(weather["weather"][0]["id"])
    if condition_code < 700:
        will_rain = True
        dt = weather["dt_txt"]
if will_rain:
    client = Client(account_sid, auth_token)
    message = client.messages.create(
        body="It's going to rain. Bring an umbrella☂︎",
        from_='+18329240661',
        to=send_to
    )
    print(message.status)
