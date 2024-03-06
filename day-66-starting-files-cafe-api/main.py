from flask import Flask, jsonify, render_template, request
from flask_sqlalchemy import SQLAlchemy
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy import Integer, String, Boolean
from random import choice

'''
Install the required packages first: 
Open the Terminal in PyCharm (bottom left). 

On Windows type:
python -m pip install -r requirements.txt

On MacOS type:
pip3 install -r requirements.txt

This will install the packages from requirements.txt for this project.
'''

app = Flask(__name__)

# CREATE DB
class Base(DeclarativeBase):
    pass
# Connect to Database
app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///cafes.db'
db = SQLAlchemy(model_class=Base)
db.init_app(app)


# Cafe TABLE Configuration
class Cafe(db.Model):
    id: Mapped[int] = mapped_column(Integer, primary_key=True)
    name: Mapped[str] = mapped_column(String(250), unique=True, nullable=False)
    map_url: Mapped[str] = mapped_column(String(500), nullable=False)
    img_url: Mapped[str] = mapped_column(String(500), nullable=False)
    location: Mapped[str] = mapped_column(String(250), nullable=False)
    seats: Mapped[str] = mapped_column(String(250), nullable=False)
    has_toilet: Mapped[bool] = mapped_column(Boolean, nullable=False)
    has_wifi: Mapped[bool] = mapped_column(Boolean, nullable=False)
    has_sockets: Mapped[bool] = mapped_column(Boolean, nullable=False)
    can_take_calls: Mapped[bool] = mapped_column(Boolean, nullable=False)
    coffee_price: Mapped[str] = mapped_column(String(250), nullable=True)

    def to_dict(self):
        # dictionary = {}
        # for column in self.__table__.columns:
        #     dictionary[column.name] = getattr(self, column.name)
        # return dictionary
        return {column.name: getattr(self, column.name) for column in self.__table__.columns}


# with app.app_context():
#     db.create_all()

@app.route("/")
def home():
    return render_template("index.html")


# HTTP GET - Read Record
@app.route("/all")
def get_all():
    all_cafes = db.session.execute(db.select(Cafe).order_by(Cafe.name)).scalars()
    return jsonify(cafe=[cafe.to_dict() for cafe in all_cafes])


@app.route("/random")
def get_one():
    result = db.session.execute(db.select(Cafe))
    all_cafes = result.scalars().all()
    random_cafe = choice(all_cafes)
    return jsonify(cafe=random_cafe.to_dict())
        # return jsonify(
        #     cafe={
        #         "id": selected_cafe.id,
        #         "name": selected_cafe.name,
        #         "map_url": selected_cafe.map_url,
        #         "img_url": selected_cafe.img_url,
        #         "location": selected_cafe.location,
        #         "seats": selected_cafe.seats,
        #         "has_toilet": selected_cafe.has_toilet,
        #         "has_wifi": selected_cafe.has_wifi,
        #         "has_sockets": selected_cafe.has_sockets,
        #         "can_take_calls": selected_cafe.can_take_calls,
        #         "coffee_price": selected_cafe.coffee_price,
        #     }
        # )

@app.route("/search")
def search():
    query_location = request.args.get("loc")
    result = db.session.execute(db.select(Cafe).where(Cafe.location == query_location))
    all_cafes = result.scalars().all()
    if all_cafes:
        return jsonify(cafe=[cafe.to_dict() for cafe in all_cafes])
    else:
        return jsonify(error={"Not Found": "Sorry, we don't have a cafe at that location"}), 404

# HTTP POST - Add Record
@app.route("/add", methods=["POST"])
def add():
    new_cafe = Cafe(
        name=request.form.get("name"),
        map_url=request.form.get("map_url"),
        img_url=request.form.get("img_url"),
        location=request.form.get("loc"),
        has_sockets=bool(request.form.get("sockets")),
        has_toilet=bool(request.form.get("toilet")),
        has_wifi=bool(request.form.get("wifi")),
        can_take_calls=bool(request.form.get("calls")),
        seats=request.form.get("seats"),
        coffee_price=request.form.get("coffee_price"),
    )
    db.session.add(new_cafe)
    db.session.commit()
    return jsonify(response={"sucess": "Sucessfully added the new cafe."}), 200
# HTTP PUT/PATCH - Update Record
@app.route("/update-price/<int:cafe_id>", methods=["PATCH"])
def update_price(cafe_id):
    cafe_to_update = db.get_or_404(Cafe, ident=cafe_id)
    if cafe_to_update:
        new_price = request.args.get("new_price")
        cafe_to_update.coffee_price = new_price
        db.session.commit()
        return jsonify(response={"sucess": "Sucessfully updated the price."})
    else:
        return jsonify(response={"error": "Sorry the cafe with that id was not found in the database"}), 400


# HTTP DELETE - Delete Record
@app.route("/report-closed/<int:cafe_id>", methods=["DELETE"])
def delete_cafe(cafe_id):
    if request.args.get("api-key") == "aprilsapikey":
        cafe_to_delete = db.get_or_404(Cafe, cafe_id)
        if cafe_to_delete:
            db.session.delete(cafe_to_delete)
            db.session.commit()
            return jsonify(response={"Sucess": "You've deleted the cafe successfully"}), 200
        else:
            return jsonify(response={"error": "Sorry the cafe with the id provided was not found"}), 404
    else:
        return jsonify(response={"error": "That's not allowed, make sure you have the correct api-key"}), 403

if __name__ == '__main__':
    app.run(debug=True)
