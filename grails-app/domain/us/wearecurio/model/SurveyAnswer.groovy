package us.wearecurio.model

import java.io.Serializable;

class SurveyAnswer implements Serializable, Comparable{

	String code
	String answer
	SurveyAnswerType answerType

	static constraints = {
		code(blank: false, size: 5..40)
		answer(blank: false, size: 5..1000)
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof SurveyAnswer))
			return -1;
		
		return code.compareTo(((SurveyAnswer)o).code)
	}
}

enum SurveyAnswerType {
	MCQ(1),
	DESCRIPTIVE(2)
	
	final int id
	SurveyAnswerType(int id) {
		this.id = id;
	}
	public int value() { return id }
}