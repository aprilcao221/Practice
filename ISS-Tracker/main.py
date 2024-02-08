import requests
from datetime import datetime
import smtplib
import time

my_longitude = 113.570438
my_latitude = 30.968713


def right_place():
    iss_response = requests.get(url="http://api.open-notify.org/iss-now.json")
    iss_response.raise_for_status()
    iss_position = iss_response.json()['iss_position']
    iss_longitude = float(iss_position['longitude'])
    iss_latitude = float(iss_position['latitude'])
    if my_longitude - 5 < iss_longitude < my_longitude + 5 and my_latitude - 5 < iss_latitude < my_latitude + 5:
        return True


def right_time():
    time_now = int(datetime.now().hour)
    parameters = {
        'lat': my_latitude,
        'lng': my_longitude,
        'formatted': 0,
        'tzid': "Asia/Hong_Kong",
    }
    sun_response = requests.get(url="https://api.sunrise-sunset.org/json", params=parameters)
    sun_response.raise_for_status()
    sun_data = sun_response.json()
    sunrise = int(sun_data['results']['sunrise'].split("T")[1].split(":")[0])
    sunset = int(sun_data['results']['sunset'].split("T")[1].split(":")[0])
    if sunset <= time_now <= sunrise:
        return True


while True:
    time.sleep(60)
    if right_time() and right_place():
        with smtplib.SMTP("smtp.163.com", 25) as connection:
            connection.starttls()
            connection.login(user="caomancomeon@163.com", password="TJWJFXASKQGBHELS")
            connection.sendmail(from_addr="caomancomeon@163.com",
                                to_addrs="caomancomeon@gmail.com",
                                msg="Subject:Look out for the ISS\n\n")

