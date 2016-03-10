package us.wearecurio.model

import org.apache.commons.logging.LogFactory

import grails.converters.*

import us.wearecurio.utility.Utils

class PlotData {
	
	private static def log = LogFactory.getLog(this)

	Long userId
	String name
	Date created
	Date modified
	String jsonPlotData
	Boolean isSnapshot

	static final int MAXPLOTDATALENGTH = 1000000

	static constraints = {
		jsonPlotData(maxSize:MAXPLOTDATALENGTH)
		isSnapshot(nullable:true)
		name(nullable:true)
	}
	
	static mapping = {
		version false
		table 'plot_data'
		userId column: 'user_id', index:'user_id_index'
		name column:'name', index:'name_index'
		jsonPlotData column:'json_plot_data', sqlType: 'mediumtext'
		isSnapshot column: 'is_snapshot'
	}
	
	static create(User user, String name, String jsonPlotData, Boolean isSnapshot) {
		log.debug "PlotData.create() userId:" + user.getId() + ", name:" + name + ", jsonPlotData(length):" + jsonPlotData.length() + ", isSnapshot:" + isSnapshot

		def plotData = JSON.parse(jsonPlotData)

		plotData.username = user.getUsername()
		
		return new PlotData(user.getId(), name, (String) plotData.encodeAsJSON(), isSnapshot)
	}
	
	static delete(PlotData plotData) {
		log.debug "PlotData.delete() plotDataId:" + plotData.getId()
		plotData.delete(flush: true)
	}

	static deleteId(long id) {
		log.debug "PlotData.deleteId() id:" + id
		PlotData plotData = PlotData.get(id)
		plotData.delete(flush: true)
	}

	PlotData() {
	}	
	
	PlotData(Long userId, String name, String jsonPlotData, boolean isSnapshot) {
		Date now = new Date()
		
		this.userId = userId
		this.name = name
		this.created = now
		this.modified = now
		this.jsonPlotData = Utils.zip(jsonPlotData)
		this.isSnapshot = isSnapshot
	}
	
	def recompressAndSave() {
		if (this.jsonPlotData.startsWith("{")) {
			this.jsonPlotData = Utils.zip(this.jsonPlotData)
			Utils.save(this, true)
		}
	}
	
	String fetchJsonPlotData() {
		return Utils.unzip(this.jsonPlotData)
	}
	
	/**
	 * Get data for the plot data lists
	 */
	def getJSONDesc() {
		return [
			id:this.id,
			name:this.name,
			created:this.created,
			modified:this.modified
		]
	}

	// for now, plots are either dynamic or snapshots
	def getIsDynamic() {
		return !getIsSnapshot()
	}
	
	String toString() {
		return "PlotData(id:" + getId() + ", name:" + name + ", created:" + Utils.dateToGMTString(created) + ", isSnapshot:" + isSnapshot + ")"
	}
}
