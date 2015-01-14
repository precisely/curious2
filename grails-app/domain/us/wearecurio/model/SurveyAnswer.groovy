package us.wearecurio.model

class SurveyAnswer implements Comparable{

	String code
	String answer
	SurveyAnswerType answerType
	Integer priority

	static constraints = {
		code(blank: false, size: 1..40)
		answer(blank: false, size: 1..1000)
	}

	static mapping = {
		id column : 'id', index:'id_index'
		code column : 'code', index:'code_index'
	}

	@Override
	public int compareTo(Object o) {
		if (!(o instanceof SurveyAnswer))
			return -1;
		
		return ((SurveyAnswer)o).priority.compareTo(priority)
	}

	public static enum SurveyAnswerType {
		MCQ(1),
		DESCRIPTIVE(2)
		
		final int id
		SurveyAnswerType(int id) {
			this.id = id;
		}
		public int value() { return id }
	}
}
