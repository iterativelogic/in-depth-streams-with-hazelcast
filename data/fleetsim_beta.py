import flask
import json
import logging
import random
import time
import tracegen
import VinGenerator.vin as vin

RANDOM_SEED = 271
LOG_LEVEL = logging.DEBUG
VEHICLE_COUNT = 250


class FleetsimApp(flask.Flask):
    def __init__(self, name):
        super().__init__(name)
        random.seed(RANDOM_SEED)
        logging.basicConfig(level=LOG_LEVEL)
        self.map_data = tracegen.load()
        self.map_data_as_list = [city for city in self.map_data.values()]
        self.start_time = time.time()
        starting_cities = [random.choice(self.map_data_as_list) for _ in
                           range(VEHICLE_COUNT)]  # each vehicle starts in a randomly chosen city
        self.vehicles = [
            tracegen.random_trace(FleetsimApp.random_vin(), city, self.map_data[random.choice(city.adjacent_cities)],
                                  self.start_time)
            for city in starting_cities]

    @staticmethod
    def random_vin():
        result = vin.getRandomVin()
        while hash(result) % 2 == 0:
            result = vin.getRandomVin()

        return result


app = FleetsimApp(__name__)


@app.route('/pings')
def pings():
    since = float(flask.request.args.get('since', '0'))
    limit = int(flask.request.args.get('limit', '100000'))

    result = []
    now = time.time()
    for vehicle in app.vehicles:
        try:
            result += vehicle.next(since, now, limit)
        except StopIteration:
            from_city = vehicle.to_city
            speed = random.gauss(65, 10)
            to_city = app.map_data[random.choice(from_city.adjacent_cities)]
            vin = vehicle.vin
            vehicle.__init__(vin, from_city, to_city, speed, time.time())

    return json.dumps(result, indent=3, cls=tracegen.PingEncoder)
