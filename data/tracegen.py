import copy
import json
import logging
import math
import random

PING_INTERVAL = 30
MILES_PER_DEGREE_LATITUDE = 69  # rough approximation
MILES_PER_DEGREE_LONGITUDE = 54  # very roughly correct for the US latitudes 31 - 41
MAP_FILE = '/opt/project/data/mapdata.csv'
VIN_FILE = '/opt/project/data/hsqldb/vins.csv'
P_CRASH = 1.0 / 120.0  # artificially high so we can see crashes happen
P_CODE = 1.0 / 120.0
CODES_LIST = ['P0010', 'P0128', 'P0171', 'P0101', 'P0A0F', 'P2210', 'U0001']


class PingEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Ping):
            return {'vin': obj.vin, 'latitude': obj.latitude, 'longitude': obj.longitude, 'time': obj.time,
                    'sequence': obj.sequence, 'obd_codes': obj.obd_codes}
        else:
            json.JSONEncoder.default(self, obj)


class CityFacts:
    def __init__(self, name, lat, lng, city_list):
        self.name = name
        self.latitude = lat
        self.longitude = lng
        self.adjacent_cities = city_list

    def __repr__(self):
        return 'CityFacts({0},{1},{2},{3})'.format(self.name, self.latitude, self.longitude, self.adjacent_cities)


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


def load_vins(file_name):
    """
    returns a dictionary with key=vin and value={'year': year,'make': themake,'model' themodel }
    """
    result = dict()

    with open(file_name, 'r') as vin_file:
        curr_line = 0
        for line in vin_file:
            curr_line += 1
            if len(line) > 0:
                words = [w.strip() for w in line.split(',')]
                if len(words) != 4:
                    logging.warning('skipping line %d because it is malformed.', curr_line)
                    continue

                vin = words[0]
                year = int(words[1])
                make = words[2]
                model = words[3]
                result[vin] = {'year': year, 'make': make, 'model': model}

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
    def __init__(self, vin, lat, lon, time, codes):
        self.vin = vin
        self.latitude = lat
        self.longitude = lon
        self.time = time
        self.sequence = 0
        self.obd_codes = codes

    def toCSV(self):
        return '{0},{1},{2},{3}'.format(self.vin, self.latitude, self.longitude, self.time)


class Trace:
    def __init__(self, vin, from_city, to_city, mph, t_zero):
        # the sequence of pings will begin at the given time offset and repeat regularly after that
        self.vin = vin
        self.from_city = from_city
        self.to_city = to_city

        start_lat = from_city.latitude
        start_long = from_city.longitude
        end_lat = to_city.latitude
        end_long = to_city.longitude
        t = t_zero
        lat = start_lat
        lon = start_long
        distance = math.sqrt((MILES_PER_DEGREE_LATITUDE * (end_lat - start_lat)) ** 2 + (
                MILES_PER_DEGREE_LONGITUDE * (end_long - start_long)) ** 2)
        ping_count = int((3600 * distance) / (PING_INTERVAL * mph))
        latitude_step = (end_lat - start_lat) / ping_count
        longitude_step = (end_long - start_long) / ping_count

        self.pings = []
        self.next_ping_index = 0
        crashed = False
        self.pings.append(Ping(self.vin, lat, lon, t, []))
        for i in range(ping_count):
            t += PING_INTERVAL
            codes = copy.copy(self.pings[-1].obd_codes)

            if not crashed:
                lat += latitude_step
                lon += longitude_step

                if random.random() < P_CODE:
                    error_code = random.choice(CODES_LIST)
                    if error_code in codes:
                        codes.remove(error_code)
                    else:
                        codes.append(error_code)

                if random.random() < P_CRASH:
                    crashed = True
                    codes.append('B0001')  # primary air bag deployed

            self.pings.append(Ping(self.vin, lat, lon, t, codes))

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


def random_trace(vin, from_city, to_city, start_time):
    logging.debug('creating random trace from: %s to %s for %s', from_city.name, to_city.name, vin)
    speed = random.gauss(65, 10)
    start_time += random.uniform(0, PING_INTERVAL)
    return Trace(vin, from_city, to_city, speed, start_time)
