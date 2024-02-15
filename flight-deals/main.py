from data_manager import DataManager
from flight_search import FlightSearch
from flight_data import FlightData
from notification_manager import NotificationManager
from datetime import datetime
from dateutil.relativedelta import relativedelta
from pprint import pprint

flight_search = FlightSearch()
data_manager = DataManager()
pprint(data_manager.user_data)


if data_manager.user_data == "":
    data_manager.write_code()

for city in data_manager.user_data:
    if city['iataCode'] == "":
        data_manager.write_code()
    flight_data = FlightData(city['iataCode'])
    if flight_data.price < city['lowestPrice']:
        message = (f"You've got a cheap flight to {city['city']}, "
                   f"GBP{flight_data.price} "
                   f"for flight number {flight_data.to_flight_no} and flight number {flight_data.return_flight_no}"
                   f"leaving on {flight_data.leaving_on.split('T')[0]} and return on {flight_data.return_on.split('T')[0]}")
















