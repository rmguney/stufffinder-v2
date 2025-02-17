import { writable } from 'svelte/store';

const getStoredUser = () => {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem('activeUser') || null;
  }
  return null;
};

export const activeUser = writable(getStoredUser());

activeUser.subscribe((value) => {
  if (typeof localStorage !== 'undefined') {
    if (value) {
      localStorage.setItem('activeUser', value);
    } else {
      localStorage.removeItem('activeUser');
    }
  }
});
