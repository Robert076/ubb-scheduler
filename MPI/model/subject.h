#pragma once
#include <string>

class Subject
{
private:
    std::string name;
    std::string mainTeacher;
    std::string language; // Added this field

    // Frequencies (Sessions per week)
    double coursesPerWeek = 0;
    double seminarsPerWeek = 0;
    double laboratoriesPerWeek = 0;

    // Durations (Hours per session)
    int courseLength = 0;
    int seminarLength = 0;
    int laboratoryLength = 0;

    // Override for Laboratory Splits
    int labSplitOverride = 0;

public:
    Subject() {}
    Subject(std::string name) : name(name) {}

    // Getters
    std::string getName() const { return name; }
    std::string getMainTeacher() const { return mainTeacher; }
    std::string getLanguage() const { return language; } // Added Getter

    double getCoursesPerWeek() const { return coursesPerWeek; }
    double getSeminarsPerWeek() const { return seminarsPerWeek; }
    double getLaboratoriesPerWeek() const { return laboratoriesPerWeek; }

    int getCourseLength() const { return courseLength; }
    int getSeminarLength() const { return seminarLength; }
    int getLaboratoryLength() const { return laboratoryLength; }

    int getLabSplitOverride() const { return labSplitOverride; }

    // Setters
    void setMainTeacher(const std::string &t) { mainTeacher = t; }
    void setLanguage(const std::string &l) { language = l; } // Added Setter

    void setCourseConfig(double freq, int len)
    {
        coursesPerWeek = freq;
        courseLength = len;
    }
    void setSeminarConfig(double freq, int len)
    {
        seminarsPerWeek = freq;
        seminarLength = len;
    }
    void setLaboratoryConfig(double freq, int len)
    {
        laboratoriesPerWeek = freq;
        laboratoryLength = len;
    }

    void setLabSplitOverride(int overrideVal)
    {
        labSplitOverride = overrideVal;
    }
};