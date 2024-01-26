from question_model import Question
from data import question_data
from quiz_brain import QuizBrain

question_bank = []
for question in question_data:
    question_text = question["text"]
    question_answer = question["answer"]
    new_question = Question(question_text, question_answer)
    question_bank.append(new_question)

question = QuizBrain(question_bank)
while question.still_has_question():
    question.quiz()
print(f"You've completed the quiz.\nYour final score is {question.score}/{question.question_number}")
