/**
 * Common javascript file for web & mobile app.
 * 
 * Any change made here must be reflect in mobile and web app. Also
 * required to update cache.manifest.
 * 
 */
$.datepicker._gotoToday = function(id) {
	var target = $(id);
	var inst = this._getInst(target[0]);
	if (this._get(inst, 'gotoCurrent') && inst.currentDay) {
		inst.selectedDay = inst.currentDay;
		inst.drawMonth = inst.selectedMonth = inst.currentMonth;
		inst.drawYear = inst.selectedYear = inst.currentYear;
	} else {
		var date = new Date();
		inst.selectedDay = date.getDate();
		inst.drawMonth = inst.selectedMonth = date.getMonth();
		inst.drawYear = inst.selectedYear = date.getFullYear();
		// the below two lines are new
		this._setDateDatepicker(target, date);
		this._selectDate(id, this._getDateDatepicker(target));
	}
	this._notifyChange(inst);
	this._adjustDate(target);
}