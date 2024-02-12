import requests
from datetime import datetime
from requests.auth import HTTPBasicAuth
import os


WEIGHT = 60
HEIGHT = 165
AGE = 33
HOST = "https://trackapi.nutritionix.com"
exercise_endpoint = "/v2/natural/exercise"
sheety_endpoint = "https://api.sheety.co/11b4c7e30ef1ca08ab6f153c40b7dbf1/workoutTrack/sheet1"

exercise_headers = {
    'x-app-id': os.environ["exercise_app_id"],
    'x-app-key': os.environ["exercise_app_key"]
}

exercise_params = {
    'query': input("Tell me which exercise you did: "),
    'weight_kg': WEIGHT,
    'height_cm': HEIGHT,
    'age': AGE
}

today = datetime.now()

basic = HTTPBasicAuth('aprilcao', os.environ["sheety_password"])
sheety_headers = {
    'Authorization': os.environ["sheety_authorization"]
}

exercise_response = requests.post(url=f"{HOST}{exercise_endpoint}", headers=exercise_headers, json=exercise_params)
exercise_response.raise_for_status()
exercise_data = exercise_response.json()["exercises"]
for exercise in exercise_data:
    sheety_params = {
        'sheet1': {
            'date': today.strftime("%d/%m/%Y"),
            'time': today.strftime("%X"),
            'exercise': exercise['name'],
            'duration': int(exercise['duration_min']),
            'calories': exercise['nf_calories']
        }
    }
    response = requests.post(url=sheety_endpoint, json=sheety_params, auth=basic, headers=sheety_headers)







