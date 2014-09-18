package cz.sio2.liferay.dl;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletConfig;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.struts.BaseStrutsPortletAction;
import com.liferay.portal.kernel.struts.StrutsPortletAction;
import com.liferay.portal.model.ResourceAction;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portal.model.Role;
import com.liferay.portal.model.RoleConstants;
import com.liferay.portal.model.User;
import com.liferay.portal.security.permission.ActionKeys;
import com.liferay.portal.security.permission.PermissionChecker;
import com.liferay.portal.security.permission.PermissionCheckerFactoryUtil;
import com.liferay.portal.service.ResourceActionLocalServiceUtil;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.RoleServiceUtil;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.documentlibrary.model.DLFileEntry;
import com.liferay.portlet.documentlibrary.model.DLFolder;
import com.liferay.portlet.documentlibrary.service.DLFileEntryLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderLocalServiceUtil;
import com.liferay.portlet.documentlibrary.service.DLFolderServiceUtil;

public class RecursivePermissionsPortletAction extends BaseStrutsPortletAction {

	public void processAction(StrutsPortletAction originalStrutsPortletAction,
			PortletConfig portletConfig, ActionRequest actionRequest,
			ActionResponse actionResponse) throws Exception {
		System.out
				.println("processAction: Wrapped /document_library/recursive_permissions");
		long folderId = Long.parseLong(actionRequest.getParameter("folderId"));
		final User user = PortalUtil.getUser(actionRequest);
		PermissionChecker permissionChecker = PermissionCheckerFactoryUtil
				.create(user, false);

		final DLFolder entry = DLFolderServiceUtil.getFolder(folderId);

		final long companyId = PortalUtil.getCompanyId(actionRequest);

		setPermissionRecursively(companyId, permissionChecker, entry);
		// originalStrutsPortletAction.processAction(
		// originalStrutsPortletAction, portletConfig, actionRequest,
		// actionResponse);
	}

	final String[] actionKeys = new String[] { ActionKeys.VIEW,
			ActionKeys.UPDATE };

	private void setPermissionRecursively(final long companyId,
			final PermissionChecker permissionChecker, final DLFolder folder) {

		addSiteMemberPermissionsForResource(companyId, permissionChecker,
				folder.getGroupId(), folder.getPrimaryKey(),
				DLFolder.class.getName(), actionKeys);

		try {

			for (final DLFileEntry fileEntry : DLFileEntryLocalServiceUtil
					.getFileEntries(folder.getGroupId(), folder.getFolderId())) {
				addSiteMemberPermissionsForResource(companyId,
						permissionChecker, fileEntry.getGroupId(),
						fileEntry.getPrimaryKey(), DLFileEntry.class.getName(),
						actionKeys);
			}

			for (final DLFolder subFolder : DLFolderLocalServiceUtil
					.getFolders(folder.getGroupId(), folder.getFolderId())) {
				setPermissionRecursively(companyId, permissionChecker,
						subFolder);
			}
		} catch (SystemException e) {
			e.printStackTrace();
		}
	}

	private void addSiteMemberPermissionsForResource(final long companyId,
			final PermissionChecker permissionChecker, final long groupId,
			final long resourcePrimaryKey, final String resourceType,
			final String[] actionKeys) {
		// TODO
		// if (permissionChecker.hasPermission(
		// fileEntry.getGroupId(),
		// DLFileEntry.class.getName(),
		// fileEntry.getFileEntryId(),
		// ActionKeys.PERMISSIONS ) ) {
		// return;
		// }

		System.out.println("Setting permissions of " + resourcePrimaryKey
				+ " (" + resourceType + ")");

		try {
			ResourcePermission resourcePermission = null;
			final Role role = RoleServiceUtil.getRole(companyId,
					RoleConstants.SITE_MEMBER);
			try {
				resourcePermission = ResourcePermissionLocalServiceUtil
						.getResourcePermission(companyId, resourceType,
								ResourceConstants.SCOPE_INDIVIDUAL,
								resourcePrimaryKey + "", role.getRoleId());
			
				long actionIdsLong = resourcePermission.getActionIds();

				for (final String actionId : actionKeys) {
					final ResourceAction resourceAction = ResourceActionLocalServiceUtil
							.getResourceAction(resourceType, actionId);
					if (!resourcePermission.hasActionId(resourceAction.getActionId())) {
						actionIdsLong += resourceAction.getBitwiseValue();
					}
				}
				resourcePermission.setActionIds(actionIdsLong);
				ResourcePermissionLocalServiceUtil
						.updateResourcePermission(resourcePermission);
			} catch (PortalException e) {
				System.out
				.println("Resource Permission not found ... creating new.");
				ResourcePermissionLocalServiceUtil.setResourcePermissions(
						companyId, resourceType,
						ResourceConstants.SCOPE_INDIVIDUAL, resourcePrimaryKey
								+ "", role.getRoleId(), actionKeys);
			}
		} catch (PortalException | SystemException e) {
			e.printStackTrace();
		}
	}

	public String render(StrutsPortletAction originalStrutsPortletAction,
			PortletConfig portletConfig, RenderRequest renderRequest,
			RenderResponse renderResponse) throws Exception {

		System.out
				.println("render: Wrapped /document_library/recursive_permissions");

		return "/portlet/document_library/view.jsp";
		// return "/portlet/document_library/recursive_permissions.jsp";
		// return originalStrutsPortletAction.render(
		// null, portletConfig, renderRequest, renderResponse);
	}

	public void serveResource(StrutsPortletAction originalStrutsPortletAction,
			PortletConfig portletConfig, ResourceRequest resourceRequest,
			ResourceResponse resourceResponse) throws Exception {

		System.out
				.println("serveResource: Wrapped /document_library/recursive_permissions");

		// originalStrutsPortletAction.serveResource(
		// originalStrutsPortletAction, portletConfig, resourceRequest,
		// resourceResponse);
	}
}
