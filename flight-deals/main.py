from data_manager import DataManager
from flight_search import FlightSearch
from notification_manager import NotificationManager

flight_search = FlightSearch()
data_manager = DataManager()


if data_manager.user_data == "":
    data_manager.write_code()

for city in data_manager.user_data:
    if city['iataCode'] == "":
        data_manager.write_code()
    data = flight_search.search_flight(city['iataCode'])
    if data is None:
        continue
    if data[0]['price'] < city['lowestPrice']:
        message = (f"You've got a cheap flight to {city['city']}, "
                   f"GBP{data[0]['price']} "
                   f"for flight number {data[0]['route'][0]['flight_no']} and flight number {data[0]['route'][1]['flight_no']}"
                   f"leaving on {data[0]['route'][0]['utc_departure'].split('T')[0]} and return on {data[0]['route'][0]['utc_departure'].split('T')[0]}")
        NotificationManager(message)
















