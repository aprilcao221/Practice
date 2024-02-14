import os
import requests
from datetime import datetime
from dateutil.relativedelta import relativedelta

START_CITY = "LON"
CURRENTY = "GBP"
TIME_PERIOD_MONTHS = 6
HEADERS = {
    'apikey': os.environ["tequila_api_key"],

}
SERVER = "https://api.tequila.kiwi.com"
location_endpoint = "/locations/query"
flight_endpoint = "/v2/search"


class FlightSearch:
    def search_location(self, city_name):
        search_params = {
            'term': city_name
        }
        response = requests.get(url=f"{SERVER}{location_endpoint}", params=search_params, headers=HEADERS)
        data = response.json()
        iata_code = data['locations'][0]['code']
        response.raise_for_status()
        return iata_code

    def search_flight(self, iata_code):
        start_date = datetime.today()
        end_date = start_date + relativedelta(months=TIME_PERIOD_MONTHS)
        return_start = start_date + relativedelta(days=7)
        return_end = start_date + relativedelta(days=28)
        search_params = {
            'fly_from': START_CITY,
            'fly_to': iata_code,
            'date_from': start_date.strftime("%d/%m/%Y"),
            'date_to': end_date.strftime("%d/%m/%Y"),
            'adults': 1,
            'curr': CURRENTY,
            'locale': "en",
            'return_from': return_start.strftime("%d/%m/%Y"),
            'return_to': return_end.strftime("%d/%m/%Y"),
            'one_for_city': 1,
            'max_stopovers': 0
        }
        response = requests.get(url=f"{SERVER}{flight_endpoint}", params=search_params, headers=HEADERS)
        response.raise_for_status()
        data = response.json()["data"]
        return data


