from data_manager import DataManager
from flight_search import FlightSearch
from flight_data import FlightData
from datetime import datetime
from dateutil.relativedelta import relativedelta
from pprint import pprint

flight_search = FlightSearch()
data_manager = DataManager()

if data_manager.iata_code == "":
    data_manager.write_code()

for city in data_manager.iata_code:
    flight_data = FlightData(city)













