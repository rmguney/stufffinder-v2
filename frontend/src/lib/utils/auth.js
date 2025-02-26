export const getAuthToken = () => {
    const token = localStorage.getItem('tokenKey');
    if (!token) return null;
    return token;
};

export const getAuthHeader = () => {
    const token = getAuthToken();
    if (!token) return {};
    return {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
    };
};
