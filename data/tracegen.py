import json
import logging
import math
import random

PING_INTERVAL = 30
MILES_PER_DEGREE_LATITUDE = 69  # rough approximation
MILES_PER_DEGREE_LONGITUDE = 54  # very roughly correct for the US latitudes 31 - 41
MAP_FILE = '/opt/project/data/mapdata.csv'


class PingEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Ping):
            return {'vin': obj.vin, 'latitude': obj.latitude, 'longitude': obj.longitude, 'time': obj.time}
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
    def __init__(self, vin, lat, lon, time):
        self.vin = vin
        self.latitude = lat
        self.longitude = lon
        self.time = time

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
        self.pings.append(Ping(self.vin, lat, lon, t))
        for i in range(ping_count):
            t += PING_INTERVAL
            lat += latitude_step
            lon += longitude_step
            self.pings.append(Ping(self.vin, lat, lon, t))

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

    def next(self, t_after, t_before, count):
        """
        Returns a (possibly empty) list of all pings that happened after t_after and before t_before
        ping_index is ignored.
        In any case, no more than count results will be returned.
        Results will be in ascending order by time.
        Raises a StopIteration exception when there are no pings that meet the criteria
        """
        result = []
        for ping in self.pings:
            if ping.time > t_after and ping.time < t_before:
                result.append(ping)
                if len(result) == count:
                    break

        if len(result) == 0:
            raise StopIteration

        return result


def random_trace(vin, from_city, to_city, start_time):
    logging.debug('creating random trace from: %s to %s', from_city.name, to_city.name)
    speed = random.gauss(65, 10)
    start_time += random.uniform(0, PING_INTERVAL)
    return Trace(vin, from_city, to_city, speed, start_time)
