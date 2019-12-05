import logging
import os.path
import random
import time
import tracegen
import VinGenerator.vin as vin

RANDOM_SEED = 271
LOG_LEVEL = logging.DEBUG
VEHICLE_COUNT = 2
CSV_OUTPUT_DIR = '/opt/project/data/alpha'


def output(vehicle_id, ping_list):
    """
    even though the vin is in every ping, phrasing the API in this way explicitly recognizes that the pings provided
    are for the same vin.  This makes it possible to avoid constantly  reopening the same output file.
    """
    with open(os.path.join(CSV_OUTPUT_DIR, '{0}.csv'.format(vehicle_id)), 'a') as outfile:
        for ping in ping_list:
            print(ping.toCSV(), sep=',', file=outfile)


def random_vin():
    result = vin.getRandomVin()
    while hash(result) % 2 == 1:
        result = vin.getRandomVin()

    return result


if __name__ == '__main__':
    random.seed(RANDOM_SEED)
    logging.basicConfig(level=LOG_LEVEL)
    map_data = tracegen.load()
    map_data_as_list = [city for city in map_data.values()]

    # create a trace for each truck - step through simulation time second by second
    start_time = time.time()
    starting_cities = [random.choice(map_data_as_list) for _ in
                       range(VEHICLE_COUNT)]  # each vehicle starts in a randomly chosen city
    vehicles = [
        tracegen.random_trace(random_vin(), city, map_data[random.choice(city.adjacent_cities)], start_time) for
        city in starting_cities]

    next_wakeup = start_time + 1
    while True:
        sleep_time = next_wakeup - time.time()
        if sleep_time > 0:
            time.sleep(sleep_time)

        now = time.time()
        for vehicle in vehicles:
            try:
                pings = vehicle.next(now)
                output(vehicle.vin, pings)

            except StopIteration:
                from_city = vehicle.to_city
                to_city = map_data[random.choice(from_city.adjacent_cities)]
                speed = random.gauss(65, 10)
                vin = vehicle.vin
                vehicle.__init__(vin, from_city, to_city, speed, time.time())

        next_wakeup += 1
