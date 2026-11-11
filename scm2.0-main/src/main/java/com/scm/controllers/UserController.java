package com.scm.controllers;
import java.util.List;
import com.scm.entities.Contact;
import com.scm.entities.User;
import com.scm.helpers.Helper;
import com.scm.services.ContactService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.UUID;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.scm.services.ImageService;



import com.scm.services.UserService;

@Controller
@RequestMapping("/user")
public class UserController {

    private Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;
    @Autowired
    private ContactService contactService;
    @Autowired
    private ImageService imageService;
    //changing the profile photo
    @PostMapping("/profile/change-photo")
    public String changeProfilePhoto(
            @RequestParam("profileImage") MultipartFile profileImage,
            @RequestParam(value = "returnTo", required = false) String returnTo,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        String redirect = safeProfileRedirect(returnTo);

        try {
            // Check whether a file was selected
            if (profileImage == null || profileImage.isEmpty()) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "Please select an image.");
                return redirect;
            }

            // Validate image type
            String contentType = profileImage.getContentType();

            if (contentType == null ||
                    !(contentType.equals("image/jpeg")
                            || contentType.equals("image/jpg")
                            || contentType.equals("image/png")
                            || contentType.equals("image/webp"))) {

                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Only JPG, PNG and WEBP images are allowed.");

                return redirect;
            }

            // Limit upload size to 5 MB
            if (profileImage.getSize() > 5 * 1024 * 1024) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage",
                        "Image size must not exceed 5 MB.");

                return redirect;
            }

            // Get the currently logged-in user
            String email = Helper.getEmailOfLoggedInUser(authentication);
            User user = userService.getUserByEmail(email);

            if (user == null) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "User not found.");
                return redirect;
            }

            // Upload image to Cloudinary
            String publicId = "profile_" + user.getUserId()
                    + "_" + UUID.randomUUID();

            String imageUrl = imageService.uploadImage(
                    profileImage, publicId);

            if (imageUrl == null || imageUrl.isBlank()) {
                redirectAttributes.addFlashAttribute(
                        "errorMessage", "Image upload failed. Please try again.");
                return redirect;
            }

            // Update only the profile picture
            user.setProfilePic(imageUrl);
            userService.updateUser(user);

            redirectAttributes.addFlashAttribute(
                    "successMessage", "Profile photo updated successfully!");

        } catch (Exception e) {
            logger.error("Error updating profile photo", e);

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to update profile photo. Please try again.");
        }

        return redirect;
    }

    private String safeProfileRedirect(String returnTo) {
        if ("/user/dashboard".equals(returnTo)) {
            return "redirect:/user/dashboard";
        }
        return "redirect:/user/profile";
    }

    // user dashbaord page




    @RequestMapping(value = "/dashboard")
    public String userDashboard(Model model, Authentication authentication) {

        String email = Helper.getEmailOfLoggedInUser(authentication);

        User user = userService.getUserByEmail(email);

        List<Contact> contacts =
                contactService.getByUserId(user.getUserId());

        long favoriteCount = contacts.stream()
                .filter(Contact::isFavorite)
                .count();

        model.addAttribute("totalContacts", contacts.size());
        model.addAttribute("favoriteCount", favoriteCount);

        model.addAttribute(
                "dashboardContacts",
                contacts.stream().limit(5).toList()
        );

        return "user/dashboard";
    }

    // user profile page


    @RequestMapping(value = "/profile")
    public String userProfile(Model model, Authentication authentication) {

        String email = Helper.getEmailOfLoggedInUser(authentication);

        User user = userService.getUserByEmail(email);

        model.addAttribute("loggedInUser", user);

        return "user/profile";
    }


    // user add contacts page

    // user view contacts

    // user edit contact

    // user delete contact

}
