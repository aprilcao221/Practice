import requests
from bs4 import BeautifulSoup


URL = "https://web.archive.org/web/20200518073855/https://www.empireonline.com/movies/features/best-movies-2/"
response = requests.get(url=URL)
content = response.text
soup = BeautifulSoup(content, "html.parser")
reversed_movies_list = [tag.getText() for tag in soup.find_all(name="h3", class_="title")]
movies_list = list(reversed(reversed_movies_list))
with open("movies.txt", "w") as file:
    for movie in movies_list:
        file.write(f"{movie}\n")






