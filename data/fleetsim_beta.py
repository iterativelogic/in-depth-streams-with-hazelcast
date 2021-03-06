import json
import logging
import random
import time

import flask

import tracegen

# RANDOM_SEED = 271
LOG_LEVEL = logging.DEBUG
VEHICLE_COUNT = 30


class InfiniteList:
    def __init__(self, capacity):
        self.backing_list = [None for _ in range(capacity)]
        self.offset = 0
        self.start_index = 0
        self.size = 0
        self.capacity = capacity

    def location_of(self, n):
        if n < 0 or n > self.size:
            raise IndexError()

        return (self.start_index + (n - self.offset)) % self.capacity

    def append(self, item):
        self.backing_list[self.location_of(self.size)] = item
        self.size += 1
        if self.size > self.capacity:
            self.offset = self.size - self.capacity
            self.start_index = self.size % self.capacity

    def slice(self, start, length):
        if length <= 0:
            raise IndexError

        if start >= self.size:
            return []

        end = start + length
        if end > self.size:
            end = self.size

        start_location = self.location_of(start)
        end_location = self.location_of(end)

        if end_location > start_location:
            return self.backing_list[start_location:end_location]
        else:
            return self.backing_list[start_location:] + self.backing_list[:end_location]


class FleetsimApp(flask.Flask):
    def __init__(self, name):
        super().__init__(name)
        random.seed()
        logging.basicConfig(level=LOG_LEVEL)

        self.map_data = tracegen.load()
        self.map_data_as_list = [city for city in self.map_data.values()]
        self.start_time = time.time()
        starting_cities = [random.choice(self.map_data_as_list) for _ in
                           range(VEHICLE_COUNT)]  # each vehicle starts in a randomly chosen city

        all_vins = tracegen.load_vins(tracegen.VIN_FILE)
        beta_vins = [vin for vin in all_vins if int(vin[-1]) % 2 == 1]
        random.shuffle(beta_vins)
        if len(beta_vins) < VEHICLE_COUNT:
            raise Exception('Insufficient VIN data available.  Decrease VEHICLE_COUNT')
        else:
            beta_vins = beta_vins[0:VEHICLE_COUNT]

        self.vehicles = [
            tracegen.random_trace(vin, city, self.map_data[random.choice(city.adjacent_cities)], self.start_time) for
            vin, city in zip(beta_vins, starting_cities)]

        self.pings = InfiniteList(120000)


app = FleetsimApp(__name__)


@app.route('/pings')
def pings():
    since = int(flask.request.args.get('since', '-1'))
    limit = int(flask.request.args.get('limit', '100000'))

    now = time.time()
    for vehicle in app.vehicles:
        try:
            vehicle_pings = vehicle.next(now)
            for ping in vehicle_pings:
                ping.sequence = app.pings.size
                app.pings.append(ping)

        except StopIteration:
            from_city = vehicle.to_city
            speed = random.gauss(65, 10)
            to_city = app.map_data[random.choice(from_city.adjacent_cities)]
            vin = vehicle.vin
            vehicle.__init__(vin, from_city, to_city, speed, time.time())

    result = app.pings.slice(since + 1, limit)
    return json.dumps(result, indent=3, cls=tracegen.PingEncoder)
