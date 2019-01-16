#!/usr/bin/env python2.6

import ssl
import xmlrpc.client
import configparser
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-f", "--from-slice-id", dest="fromSliceID",
                  help="From Slice ID, unique string")
parser.add_option("-t", "--to-slice-id", dest="toSliceID",
                  help="To Slice ID, unique string")
parser.add_option("-o", "--from-reservation", dest="fromReservation",
                  help="From Reservation GUID")
parser.add_option("-x", "--to-reservation", dest="toReservation",
                  help="To Reservation GUID")
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default="https://geni.renci.org:11443/orca/xmlrpc")
parser.add_option("-c", "--cert", dest="cert",
                  help="PEM file with cert")
parser.add_option("-p", "--private-key", dest="privateKey",
                  help="Private key file (or a PEM file if contains both private key and cert)")
parser.add_option("-e", "--secret", dest="secret",
                  help="Secret password")
(options, args) = parser.parse_args()

mandatories = ['fromSliceID', 'toSliceID', 'fromReservation', 'toReservation', 'secret' ]

for m in mandatories:
    if not options.__dict__[m]:
        print ("Mandatory option is missing\n")
        parser.print_help()
        exit(-1)

# Create an object to represent our server.
server_url = options.server;
credentials = []

props = { 'ip': '172.16.100.100' }

if server_url.startswith('https://'):
    if options.cert == None or options.privateKey == None:
        print ("For using secure (https) transport, you must specify the path to your certificate and private key")
        parser.print_help()
        exit(-1)
    # create secure transport with client cert
    context = ssl.SSLContext()
    context.load_cert_chain(options.cert, options.privateKey)
    server = xmlrpc.client.ServerProxy(server_url, context=context)
else:
    server = xmlrpc.client.ServerProxy(server_url)

# Call the server and get our result.
print ("Issuing perform slice stitch command for reservation ... \n")
result = server.orca.performSliceStitch(options.fromSliceID, options.fromReservation, options.toSliceID, options.toReservation, options.secret, props, credentials)
print (result)
