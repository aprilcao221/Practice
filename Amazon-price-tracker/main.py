import requests
from bs4 import BeautifulSoup
import smtplib

PRODUCT = "https://www.amazon.com/dp/B075CYMYK6?ref_=cm_sw_r_cp_ud_ct_FM9M699VKHTT47YD50Q6&th=1"
TARGET = 100
PSW = "[APPkey]"
header = {
    'User-Agent': "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
    'Accept-Language': "en,zh-CN;q=0.9,zh;q=0.8"
}
response = requests.get(url=PRODUCT, headers=header)
soup = BeautifulSoup(response.text, "html.parser")
price = soup.find(class_="a-offscreen").getText()
price_num = float(price.split("$")[1])

if price_num < 100:
    with smtplib.SMTP("smtp.163.com", 25) as connection:
        connection.starttls()
        connection.login(user="caomancomeon@163.com", password=PSW)
        connection.sendmail(from_addr="caomancomeon@163.com",
                            to_addrs="caomancomeon@gmail.com",
                            msg=f"Subject:You've hit the price target with {price}\n\n "
                                f"Click on this link to buy: \n{PRODUCT}"
                            )
        