import hazelcast
import hazelcast.core
import json
import logging
import math
import random
import time

VEHICLE_COUNT = 200
PING_INTERVAL = 30
RANDOM_SEED = 271
MILES_PER_DEGREE_LATITUDE = 69 # rough approximation
MILES_PER_DEGREE_LONGITUDE = 54 # very roughly correct for the US latitudes 31 - 41
HAZELCAST_MEMBERS = ['member-1:5701']
LOG_LEVEL = logging.DEBUG
MAP_FILE='/opt/project/data/mapdata.csv'


class CityFacts:
    def __init__(self, name, lat, lng, city_list):
        self.name = name
        self.latitude = lat
        self.longitude = lng
        self.adjacent_cities = city_list

    def __repr__(self):
        return 'CityFacts({0},{1},{2},{3})'.format(self.name,self.latitude,self.longitude,self.adjacent_cities)


class DMSFormatException(Exception):
    pass


def parse_dms(dms):
    """parse a string in degrees minutes seconds format (3 numbers with whitespace between)"""
    coord = dms.split()
    if len(coord) != 3:
        raise DMSFormatException()

    try:
        d = float(coord[0])
        m = float(coord[1])
        s = float(coord[2])
    except ValueError:
        raise DMSFormatException()

    if d < 0:
        result = -1.0 * (-1.0 * d + m / 60 + s / 3600)
    else:
        result = d + m / 60 + s / 3600

    return result


def load():
    result = dict()
    with open(MAP_FILE, 'r') as f:
        curr_line = 0
        for line in f:
            ++curr_line
            if len(line) > 0:
                words = [w.strip() for w in line.split(',')]
                if len(words) < 4:
                    logging.warning('Skipping line %d because it contains less than 4 fields.', curr_line)
                    continue

                name = words[0]
                try:
                    lat = parse_dms(words[1])
                    lng = parse_dms(words[2])
                except DMSFormatException:
                    logging.warning('Skipping line %d because the lat/lon coordinates could not be parsed', curr_line)

            adjacent_cities = words[3:]
            result[name] = CityFacts(name, lat, lng, adjacent_cities)
            logging.debug('loaded %s', result[name])

    return result


class Ping:
    def __init__(self, id, lat, lon, time):
        self.id = id
        self.latitude = lat
        self.longitude = lon
        self.time = time

    def toJSON(self):
        return json.dumps({'id': self.id, 'latitude': self.latitude, 'longitude': self.longitude, 'time' : self.time })

class Trace:
    def __init__(self, from_city, to_city, mph, t_zero):
        # the sequence of pings will begin at the given time offset and repeat regularly after that
        self.from_city = from_city
        self.to_city = to_city

        start_lat = from_city.latitude
        start_long = from_city.longitude
        end_lat = to_city.latitude
        end_long = to_city.longitude
        t = t_zero
        lat = start_lat
        lon = start_long
        distance =  math.sqrt((MILES_PER_DEGREE_LATITUDE * (end_lat - start_lat))**2 + (MILES_PER_DEGREE_LONGITUDE * (end_long - start_long))**2)
        ping_count = int((3600 * distance) / (PING_INTERVAL * mph))
        latitude_step = (end_lat - start_lat)/ping_count
        longitude_step = (end_long - start_long) / ping_count

        self.pings = []
        self.next_ping_index = 0
        self.pings.append(Ping(0,lat, lon, t))
        for i in range(ping_count):
            t += PING_INTERVAL
            lat += latitude_step
            lon += longitude_step
            self.pings.append(Ping(0,lat, lon, t))

    def next(self, t):
        """
        Returns a (possibly empty) list of all pings that happened before t and that have not been returned previously.
        Raises a StopIteration exception when the list has been exhausted
        """
        if self.next_ping_index == len(self.pings):
            raise StopIteration

        result = []
        while self.next_ping_index < len(self.pings) and self.pings[self.next_ping_index].time < t:
            result.append(self.pings[self.next_ping_index])
            self.next_ping_index += 1

        return result



def random_trace(from_city, start_time):
    if from_city is None:
        from_city = random.choice(map_data_aslist)


    to_city = map_data[random.choice(from_city.adjacent_cities)]

    logging.debug('creating random trace from: %s to %s',from_city.name, to_city.name)
    speed = random.gauss(65,10)
    start_time += random.uniform(0,PING_INTERVAL)
    return Trace(from_city, to_city, speed, start_time)


if __name__ == '__main__':
    random.seed(RANDOM_SEED)
    logging.basicConfig(level=LOG_LEVEL)
    map_data = load()
    map_data_aslist = [city for city in map_data.values()]

    # configure the Hazelcast client
    hz_config = hazelcast.ClientConfig()
    hz_config.network_config.addresses = HAZELCAST_MEMBERS

    # retry up to 10 times, waiting 5 seconds between connection attempts
    hz_config.network_config.connection_timeout = 5
    hz_config.network_config.connection_attempt_limit = 10
    hz_config.network_config.connection_attempt_period = 5
    hz = hazelcast.HazelcastClient(hz_config)
    position_map = hz.get_map('positions')

    # create a trace for each truck - step through simulation time second by second
    start_time = time.time()
    vehicles = []
    for i in range(VEHICLE_COUNT):
        vehicles.append(random_trace(None,start_time))

    next_wakeup = start_time + 1
    while True:
        sleep_time = next_wakeup - time.time()
        if sleep_time > 0:
            time.sleep(sleep_time)

        now = time.time()
        for i in range(len(vehicles)):
            try:
                pings = vehicles[i].next(now)
                for ping in pings:
                    ping.id = i
                    position_map.put(i, hazelcast.core.HazelcastJsonValue(ping.toJSON()))
                    logging.debug('sent %s', ping.toJSON())

            except StopIteration:
                from_city = vehicles[i].to_city
                vehicles[i] = random_trace(from_city,time.time())


        next_wakeup += 1
