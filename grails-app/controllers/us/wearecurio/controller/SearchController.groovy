package us.wearecurio.controller

import grails.converters.JSON

import java.math.MathContext
import java.text.DateFormat
import java.text.SimpleDateFormat

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.LocalDate
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter

import us.wearecurio.model.Discussion
import us.wearecurio.model.Entry
import us.wearecurio.model.Identifier
import us.wearecurio.model.PlotData
import us.wearecurio.model.Tag
import us.wearecurio.model.TagProperties
import us.wearecurio.model.TagStats
import us.wearecurio.model.TimeZoneId
import us.wearecurio.model.User
import us.wearecurio.model.UserGroup
import us.wearecurio.model.Discussion
import us.wearecurio.model.DiscussionPost
import us.wearecurio.model.Entry.RepeatType
import us.wearecurio.model.Entry.DurationType
import us.wearecurio.model.Entry.ParseAmount
import us.wearecurio.support.EntryCreateMap
import us.wearecurio.support.EntryStats
import us.wearecurio.utility.Utils

class SearchController extends LoginController {

	def searchService

	static debug(str) {
		log.debug(str)
	}

	def SearchController() {
		debug "SearchController constructor()"
	}

	def index(String type, int max, int offset) {
		User user = sessionUser()

		if (!user) {
			renderJSONGet([success: false, message: g.message(code: "auth.error.message")])
			return
		}
		params.max = max?: 5
		params.offset = offset?: 0

		if (type.equalsIgnoreCase("people")) {
			renderJSONGet(searchService.getPeopleList(user, offset, max))
			return
		} else if (type.equalsIgnoreCase("discussions")) {
			renderJSONGet(searchService.getDiscussionsList(user, params))
			return
		} else if (type.equalsIgnoreCase("sprints")) {
			renderJSONGet(searchService.getSprintsList(user, offset, max))
			return
		} else if (type.equalsIgnoreCase("allFeeds")) {
			List listItems = []

			Map sprints = searchService.getSprintsList(user, offset, max)
			if (sprints.listItems) {
				listItems.addAll(sprints.listItems.sprintList)
			}

			Map discussions = searchService.getDiscussionsList(user, params)
			if (discussions.listItems) {
				listItems.addAll(discussions.listItems.discussionList)
			}

			Map peoples = searchService.getPeopleList(user, offset, max)
			if (peoples.listItems) {
				listItems.addAll(peoples.listItems)
			}

			if (!listItems) {
				renderJSONGet([success: true, listItems: false])
				return
			}
			renderJSONGet([listItems: listItems, success: true])
			return
		}
	}

	def listData() {
		debug "SearchController.listData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to load list of search results " + user.getId()

		def c = PlotData.createCriteria()

		def entries = c {
			eq("userId", user.getId())
			not {
				eq("isSnapshot", true)
			}
			order("created", "asc")
		}

		for (entry in entries) {
			debug "Found " + entry
		}

		renderJSONGet(Utils.listJSONDesc(entries))
	}

	def savePlotData() {
		debug "DataController.savePlotData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		try {
			def plotDataObj = PlotData.createOrReplace(user, params.name, params.plotData, false)

			Utils.save(plotDataObj, true)

			renderJSONPost(['success', plotDataObj.getId()])
		} catch (Exception e) {
			renderJSONPost(['error'])
		}
	}

	def loadPlotDataId() {
		debug "DataController.loadPlotDataId() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to load plot data " + params.id

		def plotData = PlotData.get(Long.valueOf(params.id))

		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}

		debug "PlotData: " + plotData.getJsonPlotData()

		if (!plotData.getIsDynamic()) {
			renderStringGet('Not a live graph')
			return;
		}

		renderDataGet(plotData.getJsonPlotData())
	}

	def deletePlotDataId() {
		debug "DataController.deletePlotDataId() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete plot data " + params.id

		def plotData = PlotData.get(Long.valueOf(params.id))

		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}

		if (!plotData.getIsDynamic()) {
			renderStringGet('Not a live graph')
			return;
		}

		try {
			PlotData.delete(plotData)

			renderStringGet('success')
		} catch (Exception e) {
			renderStringGet('error')
		}
	}

	def listSnapshotData() {
		debug "DataController.listSnapshotData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to load list of snapshots for " + user.getId()

		def c = PlotData.createCriteria()

		def entries = c {
			eq("userId", user.getId())
			eq("isSnapshot", true)
			order("created", "asc")
		}

		for (entry in entries) {
			debug "Found " + entry
		}

		renderJSONGet(Utils.listJSONDesc(entries))
	}

	def listDiscussionData() {
		debug "DataController.listDiscussionData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		params.max = params.max ?: 10
		params.offset = params.offset ?: 0

		List groupNameList = params.userGroupNames ? params.list("userGroupNames") : []
		debug "Trying to load list of discussions for  $user.id and list:" + params.userGroupNames

		Map discussionData = groupNameList ? UserGroup.getDiscussionsInfoForGroupNameList(user, groupNameList, params) :
				UserGroup.getDiscussionsInfoForUser(user, true, params)

		debug "Found $discussionData"

		renderJSONGet(discussionData)
	}

	def listCommentData(Long discussionId, Long plotDataId) {
		debug "DataController.listCommentData() params: $params"

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		if (!discussionId && !plotDataId) {
			renderStringGet("Blank discussion call")
			return
		}
		Discussion discussion = discussionId ? Discussion.get(discussionId) : Discussion.getDiscussionForPlotDataId(plotDataId)

		if (!discussion) {
			debug "Discussion not found for id [$discussionId] or plot data id [$plotDataId]."
			renderStringGet "That discussion topic no longer exists."
			return
		}

		params.max = params.max ?: 5
		params.offset = params.offset ?: 0

		Map model = discussion.getJSONModel(params)
		model.putAll([isAdmin: UserGroup.canAdminDiscussion(user, discussion)])

		debug "Found Comment data: $model"

		renderJSONGet(model)
	}

	def saveSnapshotData() {
		debug "DataController.saveSnapshotData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Saving " + params.snapshotData

		def plotDataObj = PlotData.createOrReplace(user, params.name, params.snapshotData, true)

		Utils.save(plotDataObj, true)

		renderJSONPost([plotDataId:plotDataObj.getId()])
	}

	def loadSnapshotDataId() {
		debug "DataController.loadSnapshotDataId() params:" + params

		Long plotDataId = Long.valueOf(params.id)

		def user = sessionUser()

		if (user == null) {
			Discussion discussion = Discussion.getDiscussionForPlotDataId(plotDataId)
			if (!discussion.getIsPublic()) {
				debug "auth failure"
				renderStringGet(AUTH_ERROR_MESSAGE)
				return
			}
		}

		debug "Trying to load plot data " + params.id

		def plotData = PlotData.get(plotDataId)

		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}

		debug "PlotData: " + plotData.getJsonPlotData()

		if (!plotData.getIsSnapshot()) {
			renderStringGet('Graph is not a snapshot')
			return;
		}

		renderDataGet(plotData.getJsonPlotData())
	}

	def deleteSnapshotDataId() {
		debug "DataController.deleteSnapshotDataId() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete snapshot data " + params.id

		def plotData = PlotData.get(Long.valueOf(params.id))

		if (plotData == null) {
			renderStringGet('No such graph id ' + params.id)
			return;
		}

		if (!plotData.getIsSnapshot()) {
			renderStringGet('Graph is not a snapshot')
			return;
		}

		PlotData.delete(plotData)

		renderStringGet('success')
	}

	def setDiscussionNameData() {
		debug "DataController.setDiscussionNameData() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to set discussion name " + params.name

		def discussion = Discussion.get(Long.valueOf(params.discussionId))

		if (discussion == null) {
			renderStringGet('No such discussion id ' + params.discussionId)
			return;
		}

		if (Discussion.update(discussion, params, user)) {
			renderStringGet('success')
		} else {
			renderStringGet('Failed to update discussion name')
		}
	}

	def deleteDiscussionId() {
		debug "DataController.deleteDiscussionId() params:" + params

		def user = sessionUser()

		if (user == null) {
			debug "auth failure"
			renderStringGet(AUTH_ERROR_MESSAGE)
			return
		}

		debug "Trying to delete discussion data " + params.id

		def discussion = Discussion.get(Long.valueOf(params.id))

		if (discussion == null) {
			renderStringGet('No such discussion id ' + params.id)
			return;
		}

		Discussion.delete(discussion)

		renderStringGet('success')
	}
	
	def createDiscussionData(Long plotDataId, String name, Long id, String discussionPost) {
		def user = sessionUser()
		UserGroup group = Discussion.loadGroup(params.group, user)

		debug "DiscussionController.create to group: " + group?.dump()
		if (group) {
			Discussion discussion = Discussion.loadDiscussion(id, plotDataId, user)
			discussion = discussion ?: Discussion.create(user, name, group)

			if (discussion != null) {
				Utils.save(discussion, true)
				discussion.createPost(user, discussionPost)
				renderStringGet('success')
			} else {
				renderStringGet('fail')
			}
		}
	}
	
	def deleteCommentData(Long discussionId, Long clearPostId) {
		def user = sessionUser()
		Discussion discussion
		if (discussionId && clearPostId) {
			discussion = Discussion.get(discussionId)
			DiscussionPost.deleteComment(clearPostId, user, discussion)
			renderStringGet('success')
		} else {
			renderStringGet('fail')
		}
	}

	def createCommentData(Long discussionId, String message, Long plotIdMessage) {
		debug "Attemping to add comment '" + message + "', plotIdMessage: " + plotIdMessage
		def user = sessionUser()
		Discussion discussion = Discussion.get(discussionId)
		if (discussion) {
			def result = DiscussionPost.createComment(message, user, discussion, plotIdMessage, params)
			if (result && !(result instanceof String)) {
				renderStringGet('success')
			} else {
				renderStringGet('fail')
			}
		} else {
			renderStringGet('fail')
		}
	}
}
