#!/usr/bin/env python

import ssl
import xmlrpc.client
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-i", "--slice-id", dest="sliceID",
                  help="Slice ID, unique string")
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default='https://geni.renci.org:11443/orca/xmlrpc')
parser.add_option("-d", "--new-end-date", dest="newEndDate",
		  help="New end date in RFC3339 format (e.g. 2008-09-08T15:47:31Z)", metavar="DATE")
parser.add_option("-c", "--cert", dest="cert",
                  help="PEM file with cert")
parser.add_option("-p", "--private-key", dest="privateKey",
                  help="Private key file (or a PEM file if contains both private key and cert)")
(options, args) = parser.parse_args()

mandatories = ['newEndDate', 'sliceID']

for m in mandatories:
    if not options.__dict__[m]:
        print ("Mandatory option is missing\n")
        parser.print_help()
        exit(-1)

# Create an object to represent our server.
server_url = options.server;

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
print ("Contacting ORCA xml-rpc server to renew the lease for the sliver... \n")

if options.sliceID != None:
    sliceID = options.sliceID
else:
    f = open('sliceID.txt')
    sliceID = f.read()

newEndDate = options.newEndDate

print ("Waiting for leases in slice " + sliceID + " to be renewed until " + newEndDate + " ... \n")

credentials = []
result = server.orca.renewSlice(sliceID, credentials, newEndDate)

print (result)

if result['err'] == False and result['ret'] == True:
    print ("*** Sliver lease renewal successful")
    print ("Use Slice UID to check status of sliver or delete the sliver")
else:
    print ("*** Could not renew lease for sliver")

