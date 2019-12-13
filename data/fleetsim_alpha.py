import logging
import os.path
import random
import time
import tracegen

RANDOM_SEED = 271
LOG_LEVEL = logging.DEBUG
VEHICLE_COUNT = 3
CSV_OUTPUT_DIR = '/opt/project/data/alpha'


def output(vehicle_id, ping_list):
    """
    even though the vin is in every ping, phrasing the API in this way explicitly recognizes that the pings provided
    are for the same vin.  This makes it possible to avoid constantly  reopening the same output file.
    """
    with open(os.path.join(CSV_OUTPUT_DIR, '{0}.csv'.format(vehicle_id)), 'a') as outfile:
        for ping in ping_list:
            print(ping.toCSV(), sep=',', file=outfile)


if __name__ == '__main__':
    random.seed(RANDOM_SEED)
    logging.basicConfig(level=LOG_LEVEL)
    map_data = tracegen.load()
    map_data_as_list = [city for city in map_data.values()]

    # create a trace for each truck - step through simulation time second by second
    start_time = time.time()
    # each vehicle starts in a randomly chosen city
    starting_cities = [random.choice(map_data_as_list) for _ in range(VEHICLE_COUNT)]

    all_vins = tracegen.load_vins(tracegen.VIN_FILE)
    alpha_vins = [vin for vin in all_vins if int(vin[-1]) % 2 == 0]
    random.shuffle(alpha_vins)
    if len(alpha_vins) < VEHICLE_COUNT:
        raise Exception('Insufficient VIN data available.  Decrease VEHICLE_COUNT')
    else:
        alpha_vins = alpha_vins[0:VEHICLE_COUNT]

    vehicles = [tracegen.random_trace(vin, city, map_data[random.choice(city.adjacent_cities)], start_time) for
                city, vin in zip(starting_cities, alpha_vins)]

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
