import requests
from datetime import datetime


USERNAME = "aprilcao"
TOKEN = "Safhfiue45"
GRAPH_ID = "graph1"
pixela_endpoint = "https://pixe.la/v1/users"
user_prams = {
    'token': TOKEN,
    'username': USERNAME,
    'agreeTermsOfService': "yes",
    'notMinor': "yes",
}
graph_endpoint = f"{pixela_endpoint}/{USERNAME}/graphs"
graph_config = {
    'id': "graph1",
    'name': "Coding Graph",
    'unit': "Minutes",
    'type': "int",
    'color': "ichou"
}
headers = {
    'X-USER-TOKEN': TOKEN
}

today = datetime(year=2024, month=2, day=10)
# response = requests.post(url=graph_endpoint, json=graph_config, headers=headers)
# print(response.text)
post_value = {
    'date': today.strftime("%Y%m%d"),
    'quantity': "120",
}

put_value = {
    'quantity': "120",
}
# response = requests.post(url=f"{graph_endpoint}/{GRAPH_ID}", json=post_value, headers=headers)
# print(response.text)
# response = requests.put(url=f"{graph_endpoint}/{GRAPH_ID}/{today.strftime('%Y%m%d')}", json=put_value, headers=headers)
# print(response.text)
response = requests.delete(url=f"{pixela_endpoint}/{USERNAME}/graphs/{GRAPH_ID}/{today.strftime('%Y%m%d')}", headers=headers)
print(response.text)
