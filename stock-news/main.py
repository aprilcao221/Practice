import os
import requests
from twilio.rest import Client


STOCK = "TSLA"
COMPANY_NAME = "Tesla Inc"
account_sid = os.environ.get("account_sid")
auth_token = os.environ.get("auth_token")


def get_fluctuate():
    stock_parameters = {
        'function': "TIME_SERIES_DAILY",
        'symbol': f"{STOCK}.LON",
        'outputsize': "compact",
        'apikey': os.environ.get("news_api_key")

    }
    stock_response = requests.get(url="https://www.alphavantage.co/query", params=stock_parameters)
    stock_response.raise_for_status()
    stock_data = list(stock_response.json()['Time Series (Daily)'].values())
    yesterday_close = float(stock_data[0]['4. close'])
    before_close = float(stock_data[1]['4. close'])
    return round((yesterday_close - before_close)/yesterday_close, 4)


def get_news():
    news_parameters = {
        'apiKey': os.environ.get("news_api_key"),
        'q': "Tesla",
        'searchIn': {"title", "description"},
        'language': "en"
    }
    news_response = requests.get(url="https://newsapi.org/v2/everything", params=news_parameters)
    articles = news_response.json()["articles"]
    news_dict = {articles[i]["title"]: articles[i]["description"] for i in range(0, 3)}
    message_body = ""
    for title in news_dict:
        message_body += f"Headline: {title}\nBrief: {news_dict[title]}\n"
    return message_body


fluctuation = get_fluctuate()
if abs(fluctuation) > 0.05:
    if fluctuation > 0:
        symbol = "🔺"
    else:
        symbol = "🔻"
    client = Client(account_sid, auth_token)
    message = client.messages.create(
        body=f"TSLA: {symbol}{abs(fluctuation)}\n {get_news()}",
        from_='+18329240661',
        to='+8613128840500'
    )
