from flight_search import FlightSearch


class FlightData:
    def __init__(self, iata_code):
        self.search = FlightSearch()
        self.flight_data = self.search.search_flight(iata_code)
        self.price = self.flight_data[0]['price']
        self.route = self.flight_data[0]['route']
        self.to_airline = self.route[0]['airline']
        self.to_flight_no = self.route[0]['flight_no']
        self.destination = self.route[0]['cityTo']
        self.return_airline = self.route[1]['airline']
        self.return_flight_no = self.route[1]['flight_no']
        self.leaving_on = self.route[0]['utc_departure']
        self.return_on = self.route[1]['utc_departure']

