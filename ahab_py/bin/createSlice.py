#!/usr/bin/env python

import ssl
import xmlrpc.client
from optparse import OptionParser

parser = OptionParser()
parser.add_option("-r", "--request", dest="request",
                  help="NDL request fielname", metavar="FILE")
parser.add_option("-s", "--server", dest="server",
                  help="XMLRPC server URL", metavar="URL", default='https://geni.renci.org:11443/orca/xmlrpc')
parser.add_option("-k", "--user-key", dest="key",
                  help="User ssh key")
parser.add_option("-i", "--slice-id", dest="sliceID",
                  help="Slice id, unique string")
parser.add_option("-c", "--cert", dest="cert",
                  help="PEM file with cert")
parser.add_option("-p", "--private-key", dest="privateKey",
                  help="Private key file (or a PEM file if contains both private key and cert)")
(options, args) = parser.parse_args()


mandatories = ['request', 'sliceID']

for m in mandatories:
    if not options.__dict__[m]:
        print ("Mandatory option is missing\n")
        parser.print_help()
        exit(-1)

ndlReq = None
f = open(options.request,'r')
ndlReq = f.read()
# this is Python2.5 and above
#with open(options.request) as f:
#    ndlReq = f.read()

if ndlReq != None:
    print ("Request NDL = \n")
    print (ndlReq)
else:
    exit

users = []
if options.key != None:
    # construct a 1-element array of urn/keys
    #with open(options.key, 'r') as f:
    #    userKey = f.read()
    f = open(options.key, 'r')
    userKey = f.read()
    if userKey != None:
        users=[ {'login':'root',
               'keys': [ userKey ] } ]
    print ("User " + users[0]['login'] + " key " + users[0]['keys'][0])

# Call the server and get our result.
print ("Contacting ORCA xml-rpc server " + options.server + " for creating the sliver... \n")

# Create an object to represent our server.
server_url = options.server;

if server_url.startswith('https://'):
    if options.cert == None or options.privateKey == None:
        print ("For using secure (https) transport, you must specify the path to your certificate and private key")
        parser.print_help()
        exit(-1)
    context = ssl.SSLContext()
    context.load_cert_chain(options.cert, options.privateKey)
    server = xmlrpc.client.ServerProxy(server_url, context=context)
else:
    server = xmlrpc.client.ServerProxy(server_url)

print ("Waiting for sliver details...\n")

credentials=[]

result = server.orca.createSlice(options.sliceID, credentials, ndlReq, users)

print (result)

print ("Use Slice UID to check status of the sliver, renew sliver lease or delete the sliver \n")
