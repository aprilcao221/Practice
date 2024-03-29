import os
import requests
from flight_search import FlightSearch

HEADERS = {
    'Authorization': f"Bearer {os.environ['sheety_token']}"
}
URL = "https://api.sheety.co/11b4c7e30ef1ca08ab6f153c40b7dbf1/flightDeals/prices"


class DataManager:
    def __init__(self):
        self.response = requests.get(url=URL, headers=HEADERS)
        self.user_data = self.response.json()['prices']
        self.search = FlightSearch()

    def write_code(self):
        for city in self.user_data:
            updates = {
                'prices': {
                    'iataCode': self.search.search_location(city['city'])
                }
            }
            response = requests.put(url=f"{URL}/{city['id']}", json=updates, headers=HEADERS)
            print(response.text)








