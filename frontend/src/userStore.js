import { writable } from 'svelte/store';
import { browser } from '$app/environment';

// Cookie utility functions
function getCookie(name) {
  if (!browser) return null;
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

function setCookie(name, value) {
  if (!browser) return;
  // Set cookie with 1 hour expiration time
  const expirationDate = new Date();
  expirationDate.setTime(expirationDate.getTime() + (60 * 60 * 1000)); // 1 hour in milliseconds
  document.cookie = `${name}=${value}; path=/; expires=${expirationDate.toUTCString()}; SameSite=Strict`;
}

function deleteCookie(name) {
  if (!browser) return;
  document.cookie = `${name}=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT`;
}

// Initialize store from cookie instead of localStorage
const storedUser = browser ? getCookie('currentUser') : null;
export const activeUser = writable(storedUser);

// Subscribe to changes and update cookie
if (browser) {
  activeUser.subscribe(value => {
    if (value) {
      setCookie('currentUser', value);
    } else {
      deleteCookie('currentUser');
      deleteCookie('tokenKey');
      deleteCookie("userRole")
    }
  });
}