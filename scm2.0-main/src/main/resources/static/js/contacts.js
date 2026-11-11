console.log("Contacts.js");

const viewContactModal = document.getElementById("view_contact_modal");

const options = {
  placement: "center",
  backdrop: "dynamic",
  backdropClasses: "bg-gray-900/50 dark:bg-gray-900/80 fixed inset-0 z-40",
  closable: true,
};

const instanceOptions = {
  id: "view_contact_modal",
  override: true,
};

const contactModal = viewContactModal ? new Modal(viewContactModal, options, instanceOptions) : null;

function openContactModal() {
  if (contactModal) {
    contactModal.show();
  }
}

function closeContactModal() {
  if (contactModal) {
    contactModal.hide();
  }
}

function showText(selector, value) {
  const element = document.querySelector(selector);
  if (!element) {
    return;
  }
  const text = value != null && String(value).trim() !== "" ? String(value) : "Not provided";
  element.textContent = text;
}

function showLink(selector, value) {
  const element = document.querySelector(selector);
  if (!element) {
    return;
  }
  if (value != null && String(value).trim() !== "") {
    const link = String(value).trim();
    element.textContent = link;
    element.href = /^https?:\/\//i.test(link) ? link : "https://" + link;
    element.target = "_blank";
  } else {
    element.textContent = "Not provided";
    element.removeAttribute("href");
  }
}

async function loadContactdata(id) {
  try {
    const response = await fetch("/api/contacts/" + encodeURIComponent(id));
    if (!response.ok) {
      throw new Error("Could not load contact");
    }
    const data = await response.json();

    showText("#contact_name", data.name);
    showText("#contact_email", data.email);
    showText("#contact_phone", data.phoneNumber);
    showText("#contact_address", data.address);
    showText("#contact_about", data.description);

    const image = document.querySelector("#contact_image");
    if (image) {
      image.src = data.picture && String(data.picture).trim() !== ""
        ? data.picture
        : "https://static-00.iconduck.com/assets.00/profile-default-icon-2048x2045-u3j7s5nj.png";
    }

    const contactFavorite = document.querySelector("#contact_favorite");
    if (contactFavorite) {
      contactFavorite.textContent = data.favorite ? "Yes" : "No";
    }

    showLink("#contact_website", data.websiteLink);
    showLink("#contact_linkedIn", data.linkedInLink);
    openContactModal();
  } catch (error) {
    console.log("Error: ", error);
    Swal.fire({
      icon: "error",
      title: "Could not open this contact",
      text: "The details could not be loaded. Try again.",
    });
  }
}

async function deleteContact(id) {
  Swal.fire({
    title: "Do you want to delete the contact?",
    icon: "warning",
    showCancelButton: true,
    confirmButtonText: "Delete",
  }).then((result) => {
    if (result.isConfirmed) {
      window.location.assign("/user/contacts/delete/" + encodeURIComponent(id));
    }
  });
}
