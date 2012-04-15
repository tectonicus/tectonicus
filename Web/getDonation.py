#!/usr/bin/python
	
import base64
import urllib2
import sys
from xml.dom import minidom
	
print "Content-Type: text/plain\n"
	
import cgi
form = cgi.FieldStorage()
if 'username' not in form or 'json_callback' not in form:
	print 'username not specified'
else:
	
	nameToFind = form['username'].value
	jsonCallback = form['json_callback'].value
	
	# justgiving.com application id
	applicationId = 'ba6a9f9d'
	
	# Urls
	stagingUrl = 'https://api.staging.justgiving.com/'
	liveUrl = 'https://api.justgiving.com/'
	
	baseUrl = liveUrl
	
	requestUrl = baseUrl + applicationId + '/v1/fundraising/pages/tectonicus/donations?PageSize=150'
	
	#credentials = 'basic ' + base64.standard_b64encode( username + ':' + password )
	credentials = 'basic T3Jhbmd5VGFuZzp0cmlncmFwaA=='
	

	
	foundAmount = 0
	
	# Hardcoded map of people who donated but not with their minecraft id
	otherDonators = { 'griffen8280':20, 'Vyruz':5, 'iamsofa':10 }
	
	if nameToFind in otherDonators:
		foundAmount = otherDonators[nameToFind]
		
	else:
		#print >> sys.stderr, ("requestUrl:["+requestUrl+"]")
		
		request = urllib2.Request(requestUrl)
		request.add_header('accept', 'application/xml')
		request.add_header('authorize', credentials);
		
		opener = urllib2.build_opener()
		stream = opener.open(request) 
		data = stream.read()
	
		xmldoc = minidom.parseString(data)
		
	#	sys.stderr.write('\n***\n'+data+'\n***\n')
		
		root = xmldoc.childNodes
	
		fundraisingNode = root.item(0)
		donationsNode = fundraisingNode.getElementsByTagName('donations')[0]
	
		for donation in donationsNode.childNodes:
		
			nameNode = donation.getElementsByTagName('donorDisplayName')[0]
			if nameNode is not None:
				if nameNode.childNodes.item(0) is not None:
					name = nameNode.childNodes.item(0).data
					
					if name == nameToFind:	
						amountNode = donation.getElementsByTagName('amount')[0]
			
						amount = amountNode.childNodes.item(0).data
						foundAmount = int(float(amount)*100+0.5) # rounded to whole number of pence
						break
	
	# Now assemble the output
	
	text = ''
	icon = ''
	
	if foundAmount == 0:
		text = 'is this you? why not donate!'
		icon = 'Images/Spacer.png'
		
	elif foundAmount >= 5000:
		text = 'Diamond Supporter'
		icon = 'Images/DiamondIcon.png'
	
	elif foundAmount >= 2000:
		text = 'Gold Supporter'
		icon = 'Images/GoldIcon.png'
	
	else:
		text = 'Iron Supporter'
		icon = 'Images/IronIcon.png'
	
	
	html = ''
	html += '<center><table><tr>'
						
	html += '<td>'
	html += '<img src=\\\"' + icon + '\\\"/>'
	html += '</td>'
												
	html += '<td>'
	html += '<a href=\\\"http://www.justgiving.com/tectonicus\\\">' + text + '</a>'
	html += '</td>'
												
	html += '</tr></table></center>'
	
	# jsonp123({"name" : "Remy", "id" : "10", "blog" : "http://remysharp" });
	
	output = ''
	output += jsonCallback
	output += '( {\"html\" : \"'
	output += html
	output += '\" });'
	
	print output