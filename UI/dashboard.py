import hazelcast
import logging
import threading

from bokeh.io import curdoc
from bokeh.layouts import column
from bokeh.models import ColumnDataSource
from bokeh.models.map_plots import GMapOptions
from bokeh.plotting import gmap

LOG_LEVEL = logging.DEBUG
HAZELCAST_MEMBERS = ['jet-server-1:5701','jet-server-2:5701']

logging.basicConfig(level=LOG_LEVEL)

# set up Hazelcast connection
hz_config = hazelcast.ClientConfig()
hz_config.network_config.addresses = HAZELCAST_MEMBERS

# retry up to 10 times, waiting 5 seconds between connection attempts
hz_config.network_config.connection_timeout = 5
hz_config.network_config.connection_attempt_limit = 10
hz_config.network_config.connection_attempt_period = 5
hz = hazelcast.HazelcastClient(hz_config)

# position map and lock
vehicle_map = hz.get_map('vehicles')  # remote hz map
vehicle_map_lock = threading.Lock()  # if we eventually end up with multiple map servers this should be a distributed lock
change_map = dict()  # changes accumulate in this local map


# color map and lock
# color_map = hz.get_map('ping_output')  # remote hz map
# color_change_map_lock = threading.Lock()  # if we eventually end up with multiple map servers this should be a distributed lock
# color_change_map = dict()  # changes accumulate in this local map

# alpha change  map and lock, used to hide expired entries
# alpha_change_map = dict()
# alpha_change_map_lock = threading.Lock()


# add a listener to position map to place all add/update events into the changeMap
def vehicle_map_listener(event):
    global change_map
    with vehicle_map_lock:
        change_map[event.key] = event.value


vehicle_map.add_entry_listener(include_value=True, updated_func=vehicle_map_listener, added_func=vehicle_map_listener)


def colormap(entry):
    if 'status' not in entry or entry['status'] == '':
        return 'blue'
    elif entry['status'] == 'CRASHED':
        return 'red'
    elif entry['status'] == 'SELECTED':
        return 'green'
    else:
        return 'yellow'


def sizemap(entry):
    if 'status' not in entry or entry['status'] == '' or entry['status']=='SELECTED':
        return 6
    else:
        return 10

# now retrieve all entries from the map and build a ColumnDataSource

# apparently map.values() returns a concurrent.futures.Future
values = [entry.loads() for entry in vehicle_map.values().result()]
latitudes = [entry['latitude'] for entry in values]
longitudes = [entry['longitude'] for entry in values]
colors = [colormap(entry) for entry in values]
sizes = [sizemap(entry) for entry in values]

data_source = ColumnDataSource({'latitude': latitudes, 'longitude': longitudes, 'color': colors, 'size': sizes})

# map each vin to a certain position within the lists
id_to_index_map = {item['vin']: i for (i, item) in enumerate(values)}

# build the map
louisville_lat = 38 + 15 / 60 + 9 / 3600
louisville_long = -1 * (85 + 18 / 60 + 41 / 3600)

map_options = GMapOptions(map_type='roadmap', lat=louisville_lat, lng=louisville_long, zoom=6)
p = gmap("AIzaSyDiRy33QAJj4gMedAs8Cde6e3-bER68Htw", map_options, title='US')
p.circle(x='longitude', y='latitude', color='color', size='size', source=data_source)
layout = column(p)


def update():
    patches = dict()
    if len(change_map) > 0:
        with vehicle_map_lock:
            entry_list = [entry for entry in [e.loads() for e in change_map.values()] if
                          entry['vin'] in id_to_index_map]  # in check is costly, can we do something better ?
            longitude_patches = [(id_to_index_map[entry['vin']], entry['longitude']) for entry in entry_list]
            latitude_patches = [(id_to_index_map[entry['vin']], entry['latitude']) for entry in entry_list]
            color_patches = [(id_to_index_map[entry['vin']], colormap(entry)) for entry in entry_list]
            size_patches = [(id_to_index_map[entry['vin']], sizemap(entry)) for entry in entry_list]
            patches['longitude'] = longitude_patches
            patches['latitude'] = latitude_patches
            patches['size'] = size_patches
            patches['color'] = color_patches
            change_map.clear()
    else:
        logging.debug('no position changes')

    if len(patches) > 0:
        data_source.patch(patches)


curdoc().add_periodic_callback(update, 1000)
curdoc().add_root(layout)
