import requests
from twilio.rest import Client

api_key = "91184e2cbe28f7df7fa6d76e37833398"
my_longitude = 113.570438
my_latitude = 30.968713
account_sid = "ACabcbe83551f6348849670d7e36bfe944"
auth_token = "0d32b44d57adeb7b85cc5e87f5430221"



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
        to='+8613128840500'
    )
    print(message.status)
