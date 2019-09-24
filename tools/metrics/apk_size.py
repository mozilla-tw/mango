# This Source Code Form is subject to the terms of the Mozilla Public
# License, v. 2.0. If a copy of the MPL was not distributed with this
# file, You can obtain one at http://mozilla.org/MPL/2.0/.

# Check APK file size for limit

from os import path, listdir, stat
from sys import exit
import argparse

SIZE_LIMIT = 4.5 * 1024 * 1024
parser = argparse.ArgumentParser(description='Determine Path')
parser.add_argument('product', choices=['focus', 'preview'], default='focus')
parser.add_argument('engine', choices=['webkit'], default='webkit')
args = parser.parse_args()
FLAVOR = args.product + args.engine.capitalize()
PATH = path.join(path.dirname(path.abspath(__file__)), '../../app/build/outputs/apk/' + FLAVOR + '/release')

files = []
try:
    files = [f for f in listdir(PATH) if path.isfile(path.join(PATH, f)) and f.endswith('.apk') and "release" in f]
except OSError as e:
    if e.errno == 2:
        print("Directory is missing, build apk first!")
        exit(1)
    print("Unknown error: {err}".format(err=str(e)))
    exit(2)

for apk_file in files:
    file_size = stat(path.join(PATH, apk_file)).st_size
    if file_size > SIZE_LIMIT:
        print(" * [TOOBIG] {filename} ({filesize} > {sizelimit})".format(
            filename=apk_file, filesize=file_size, sizelimit=SIZE_LIMIT
        ))
        exit(27)
    print(" * [OKAY] {filename} ({filesize} <= {sizelimit})".format(
            filename=apk_file, filesize=file_size, sizelimit=SIZE_LIMIT
        ))
