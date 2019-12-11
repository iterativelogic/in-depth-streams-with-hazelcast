import json
import requests
import time

REQUIRED_VEHICLES = 13

if __name__ == '__main__':
    """
    This program uses "randomvin.com" to obtain random valid vins and "api.carmd.com" to obtain year,make and model 
    for each vin.  Output is written to a CSV file.
    """
    with open('vins.csv', 'a') as vinfile:
        count = 0
        while count < REQUIRED_VEHICLES:
            random_vin = requests.get('https://randomvin.com/getvin.php?type=fake').text.strip()

            headers = {"content-type": "application/json",
                       "authorization": "Basic ZDE4NTc1OTMtODRiNC00NWFjLTgwNjItNTgxNGFjMzQxZTQ4",
                       "partner-token": "7b71bf922b514204b4b31d28b88bf8ae"}

            vin_decode = requests.get('http://api.carmd.com/v3.0/decode', headers=headers,
                                      params={'vin': random_vin}).text
            json_response = json.loads(vin_decode)
            data = json_response['data']
            if data is not None:
                year = data['year']
                make = data['make']
                model = data['model']
                count += 1
                print(random_vin, year, make, model, sep=',', file=vinfile)
                print('{} {:4d} {:12s} {:12s}'.format(random_vin, year, make, model))

            time.sleep(2)
