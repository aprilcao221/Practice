from bs4 import BeautifulSoup
import requests

response = requests.get("https://news.ycombinator.com/")
contents = response.text
soup = BeautifulSoup(contents, "html.parser")
all_trees = soup.find_all(name="tr", class_="athing")
article_dict = {}
for tree in all_trees:
    article_id = tree.get("id")
    headline = tree.find(name="span", class_="titleline").select_one("a")
    headline_text = headline.getText()
    headline_link = headline.get("href")
    upvote = soup.find(class_="score", id=f"score_{article_id}")
    try:
        score = upvote.getText()
    except AttributeError:
        pass
    else:
        points = int(score.split(" ")[0])
        article_dict[points] = [headline_text, headline_link]
points_pool = [key for key in article_dict]
highest = max(points_pool)
article = article_dict[highest]
print(f"Highlight of the day: \n"
      f"{article[0]}\n"
      f"click this link to check for more: {article[1]}")




















# with open("website.html") as file:
#     contents = file.read()

# soup = BeautifulSoup(contents, 'html.parser')
# print(soup.prettify())
# print(soup.title)
# print(soup.title.name)
# print(soup.title.string)
# print(soup.p)
# anchor_tags = soup.find_all(name="a")

# for tag in anchor_tags:
#     # print(tag.getText())
#     print(tag.get("href"))

# heading = soup.find(name="h1", id="name")
# print(heading)

# section_heading = soup.find_all(name="h3", class_="heading")
# tags = [tag.string for tag in section_heading]
# print(tags)

# company_url = soup.select_one(selector="p a")
# print(company_url.get("href"))

# name = soup.select_one(selector="#name")
# print(name.get("id"))
#
# headings = soup.select(selector=".heading")
# print(headings)